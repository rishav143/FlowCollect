package com.flowcollect.api.confirm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Redirects legacy backend-hosted confirmation links to the frontend SPA.
 *
 * Emails sent before the frontend URL was configured may contain links like:
 *   http://localhost:8080/confirm/{token}
 *
 * This controller transparently redirects them to:
 *   {app.frontend-url}/confirm/{token}
 *
 * so customers always land on the correct UI regardless of when the link was generated.
 */
@Controller
public class ConfirmationRedirectController {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/confirm/{token}")
    public void redirect(@PathVariable String token, HttpServletResponse response) throws IOException {
        response.sendRedirect(frontendUrl + "/confirm/" + token);
    }
}
