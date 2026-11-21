package top.knos.mySeek.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestIdFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesRequestIdAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestIdFilter filter = new RequestIdFilter();

        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> mdcDuringChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertNotNull(response.getHeader(RequestIdFilter.HEADER_NAME));
        assertFalse(response.getHeader(RequestIdFilter.HEADER_NAME).isBlank());
        assertEquals(response.getHeader(RequestIdFilter.HEADER_NAME), mdcDuringChain.get());
        assertEquals(null, MDC.get(RequestIdFilter.MDC_KEY));
    }

    @Test
    void reusesRequestIdFromHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestIdFilter().doFilter(request, response, new MockFilterChain());

        assertEquals("trace-123", response.getHeader(RequestIdFilter.HEADER_NAME));
    }
}
