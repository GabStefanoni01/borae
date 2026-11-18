"""Boraê 3D Engine — first ZeroGPU proof of concept.

This Space is intentionally isolated from the Boraê API. It accepts one image,
runs the TripoSR baseline and returns a GLB plus machine-readable metadata.
The Space must be PRIVATE; the backend will authenticate with a Hugging Face
token stored only as a server-side secret.
"""
from __future__ import annotations

import logging
import os
import re
import shlex
import struct
import subprocess
import sys
import tempfile
from pathlib import Path
from uuid import UUID

import gradio as gr
import numpy as np
import spaces
import torch
from PIL import Image, ImageOps

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("borae-3d-engine")

# Pin the upstream source so Space rebuilds do not silently change the pipeline.
TRIPOSR_REPOSITORY = "https://github.com/VAST-AI-Research/TripoSR.git"
TRIPOSR_COMMIT = "107cefdc244c39106fa830359024f6a2f1c78871"
TRIPOSR_ROOT = Path("/tmp/borae-trip osr").with_name("borae-triposr")
MAX_IMAGE_PIXELS = 16_000_000
MAX_IMAGE_SIDE = 4096
MAX_GLB_BYTES = 25 * 1024 * 1024
MARCHING_CUBES_RESOLUTION = 192
GPU_FUNCTION_TIMEOUT_SECONDS = 120


def _prepare_triposr_source() -> None:
    if not (TRIPOSR_ROOT / "tsr" / "system.py").exists():
        subprocess.run(
            ["git", "clone", "--filter=blob:none", TRIPOSR_REPOSITORY, str(TRIPOSR_ROOT)],
            check=True,
            stdout=subprocess.DEVNULL,
        )
        subprocess.run(
            ["git", "-C", str(TRIPOSR_ROOT), "checkout", TRIPOSR_COMMIT],
            check=True,
            stdout=subprocess.DEVNULL,
        )

    sys.path.insert(0, str(TRIPOSR_ROOT))

    # TripoSR's marching-cubes extension must be compiled for ZeroGPU's
    # Blackwell architecture. This follows the official TripoSR Space setup.
    env = {**os.environ, "TORCH_CUDA_ARCH_LIST": "12.0+PTX"}
    subprocess.run(
        shlex.split(
            f"{sys.executable} -m pip install --no-build-isolation "
            "git+https://github.com/tatsy/torchmcubes.git"
        ),
        env=env,
        check=True,
    )


_prepare_triposr_source()

import rembg  # noqa: E402
from tsr.system import TSR  # noqa: E402
from tsr.utils import remove_background, resize_foreground, to_gradio_3d_orientation  # noqa: E402

DEVICE = "cuda:0" if torch.cuda.is_available() else "cpu"
logger.info("Loading TripoSR on %s", DEVICE)
model = TSR.from_pretrained(
    "stabilityai/TripoSR",
    config_name="config.yaml",
    weight_name="model.ckpt",
)
model.renderer.set_chunk_size(8192)
model.to(DEVICE)
background_session = rembg.new_session()


def _validate_request(image: Image.Image | None, generation_id: str) -> str:
    if image is None:
        raise gr.Error("Envie uma imagem do prato.")

    try:
        parsed_id = UUID(generation_id)
    except (TypeError, ValueError, AttributeError) as exc:
        raise gr.Error("generationId precisa ser um UUID válido.") from exc

    width, height = image.size
    if width < 256 or height < 256:
        raise gr.Error("A imagem precisa ter pelo menos 256 × 256 pixels.")
    if width > MAX_IMAGE_SIDE or height > MAX_IMAGE_SIDE:
        raise gr.Error("A imagem ultrapassa a dimensão máxima de 4096 pixels.")
    if width * height > MAX_IMAGE_PIXELS:
        raise gr.Error("A imagem ultrapassa o limite de pixels permitido.")

    return str(parsed_id)


def _preprocess_image(image: Image.Image) -> Image.Image:
    image = ImageOps.exif_transpose(image).convert("RGB")
    foreground = remove_background(image, background_session)
    foreground = resize_foreground(foreground, 0.85)

    rgba = np.asarray(foreground).astype(np.float32) / 255.0
    if rgba.ndim == 3 and rgba.shape[2] == 4:
        rgb = rgba[:, :, :3] * rgba[:, :, 3:4] + (1.0 - rgba[:, :, 3:4]) * 0.5
        return Image.fromarray((rgb * 255.0).clip(0, 255).astype(np.uint8))

    return foreground.convert("RGB")


def _validate_glb(path: str) -> int:
    file_path = Path(path)
    size = file_path.stat().st_size
    if size < 20 or size > MAX_GLB_BYTES:
        raise RuntimeError("O GLB gerado está vazio ou excede 25 MB.")

    with file_path.open("rb") as artifact:
        header = artifact.read(12)

    magic, version, declared_length = struct.unpack("<4sII", header)
    if magic != b"glTF" or version != 2 or declared_length != size:
        raise RuntimeError("O arquivo gerado não é um GLB 2.0 íntegro.")

    return size


@spaces.GPU(duration=GPU_FUNCTION_TIMEOUT_SECONDS)
def _generate_glb(prepared_image: Image.Image) -> str:
    with torch.inference_mode():
        scene_codes = model(prepared_image, device=DEVICE)
        mesh = model.extract_mesh(
            scene_codes,
            has_vertex_color=True,
            resolution=MARCHING_CUBES_RESOLUTION,
        )[0]
        mesh = to_gradio_3d_orientation(mesh)

        descriptor, output_path = tempfile.mkstemp(prefix="borae-", suffix=".glb")
        os.close(descriptor)
        mesh.export(output_path, file_type="glb")

    _validate_glb(output_path)
    return output_path


def generate_model(
    image: Image.Image | None,
    generation_id: str,
) -> tuple[dict, str]:
    """Gradio API endpoint consumed later by the Boraê backend."""
    normalized_id = _validate_request(image, generation_id)

    try:
        prepared = _preprocess_image(image)
        artifact_path = _generate_glb(prepared)
        artifact_size = _validate_glb(artifact_path)
    except gr.Error:
        raise
    except Exception as exc:
        logger.exception("3D generation failed for %s", normalized_id)
        raise gr.Error(
            "Não foi possível gerar o modelo 3D. Tente novamente mais tarde."
        ) from exc

    metadata = {
        "generationId": normalized_id,
        "status": "COMPLETED",
        "engine": "triposr",
        "format": "GLB",
        "glbVersion": 2,
        "artifactBytes": artifact_size,
        "meshColorMode": "VERTEX_COLORS",
        "sourceImageCount": 1,
    }
    return metadata, artifact_path


with gr.Blocks(title="Boraê 3D Engine") as demo:
    gr.Markdown(
        """
        # Boraê 3D Engine
        Serviço experimental de geração 3D para pratos usando TripoSR e Hugging Face ZeroGPU.

        **PoC:** uma imagem por geração. O resultado é um GLB com cores por vértice,
        não uma textura PBR pronta para produção.
        """
    )
    with gr.Row():
        input_image = gr.Image(
            label="Foto do prato",
            sources=["upload"],
            type="pil",
        )
        generation_id = gr.Textbox(
            label="Generation ID",
            placeholder="UUID criado pelo backend Boraê",
        )
    generate_button = gr.Button("Gerar modelo 3D", variant="primary")
    metadata_output = gr.JSON(label="Resultado")
    glb_output = gr.File(label="Modelo GLB")

    generate_button.click(
        fn=generate_model,
        inputs=[input_image, generation_id],
        outputs=[metadata_output, glb_output],
        api_name="generate_model",
    )

demo.queue(max_size=2)
demo.launch(max_file_size="12mb")
