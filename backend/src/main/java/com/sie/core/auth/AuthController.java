package com.sie.core.auth;

import com.sie.core.auth.dto.LoginRequest;
import com.sie.core.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The one endpoint that is reachable without a token - everything else requires Authorization: Bearer ... */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final CustomUserDetailsService userDetailsService;
  private final JwtService jwtService;

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest request) {
    // Throws BadCredentialsException (-> 401, handled by GlobalExceptionHandler) on a bad password.
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
    String token = jwtService.generateToken(userDetails);
    String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    return new LoginResponse(token, userDetails.getUsername(), role);
  }
}
