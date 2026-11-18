package com.borae.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.junit.jupiter.api.Assertions.*;

class AnonymousAssistantSessionServiceTest {

    private AnonymousAssistantSessionService service;

    @BeforeEach
    void setUp() {
        service = new AnonymousAssistantSessionService(
                "test-only-secret-with-sufficient-entropy",
                false,
                "Lax"
        );
    }

    @Test
    void issuesHttpOnlyCookieWithExpectedSecurityAttributes() {
        var issued = service.issue();
        ResponseCookie cookie = issued.cookie();

        assertEquals(AnonymousAssistantSessionService.COOKIE_NAME, cookie.getName());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals("/api/v1/public", cookie.getPath());
        assertEquals(2592000, cookie.getMaxAge().getSeconds());
        assertNotNull(issued.expiresAt());
    }

    @Test
    void acceptsIssuedCookieAndDerivesOpaqueQuotaKey() {
        String cookie = service.issue().cookie().getValue();

        assertTrue(service.isValid(cookie));
        String quotaKey = service.quotaKey(cookie);
        assertNotNull(quotaKey);
        assertNotEquals(cookie, quotaKey);
        assertEquals(quotaKey, service.quotaKey(cookie));
    }

    @Test
    void rejectsTamperedCookie() {
        String cookie = service.issue().cookie().getValue();
        String tampered = cookie.substring(0, cookie.length() - 1)
                + (cookie.endsWith("a") ? "b" : "a");

        assertFalse(service.isValid(tampered));
        assertNull(service.quotaKey(tampered));
    }

    @Test
    void rejectsMissingOrMalformedCookie() {
        assertFalse(service.isValid(null));
        assertFalse(service.isValid("not-a-session"));
        assertNull(service.quotaKey("not-a-session"));
    }

    @Test
    void issuesDifferentSessionsForDifferentBrowsers() {
        String first = service.issue().cookie().getValue();
        String second = service.issue().cookie().getValue();

        assertNotEquals(first, second);
        assertNotEquals(service.quotaKey(first), service.quotaKey(second));
    }
}
