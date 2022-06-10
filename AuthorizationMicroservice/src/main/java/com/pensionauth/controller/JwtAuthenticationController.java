package com.pensionauth.controller;

import static com.pensionauth.Constants.INVALID_CREDENTIALS;
import static com.pensionauth.Constants.USER_DISABLED;
import static com.pensionauth.Constants.VALIDITY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pensionauth.config.JwtTokenUtil;
import com.pensionauth.exception.AuthorizationException;
import com.pensionauth.model.JwtRequest;
import com.pensionauth.model.JwtResponse;
import com.pensionauth.model.TokenValidationResponse;
import com.pensionauth.service.MyUserDetailsService;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@Slf4j
@RequestMapping("/login")
public class JwtAuthenticationController{
	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationController.class);

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private MyUserDetailsService userDetailsService;

	/**
	 * @param authenticationRequest
	 * @return
	 * @throws AuthorizationException
	 * @throws Exception
	 */
	@PostMapping(value = "/authenticate")
	public ResponseEntity<JwtResponse> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest)
			throws AuthorizationException {
		log.debug("Username"+authenticationRequest.getUsername());
		log.debug("Password"+authenticationRequest.getPassword());
		Authentication auth=authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
		
		log.info(auth.toString());
		
		final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
		log.debug(userDetails.toString());
		final String token = jwtTokenUtil.generateToken(userDetails);
		return ResponseEntity.ok(new JwtResponse(token,VALIDITY));
	}

	private Authentication authenticate(String userName, String password) throws AuthorizationException {
		try {
			log.info("Inside authenticate Method==================================");
			log.debug("Username: "+userName);
			Authentication auth=authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userName, password));
			log.info("Authentication Successful.....");
			log.debug(auth.getCredentials()+"+++++++++++++++++++++++++++++++++");
			return auth;
			
		} catch (DisabledException e) {
		log.error(USER_DISABLED);
			throw new AuthorizationException(USER_DISABLED);
			
		} catch (BadCredentialsException e) {
			log.error(INVALID_CREDENTIALS);
			e.printStackTrace();
			throw new AuthorizationException(INVALID_CREDENTIALS);
		}
		
	}

	/**
	 * @param requestTokenHeader
	 * @return
	 */
	@PostMapping(value = "/authorize")
	public ResponseEntity<TokenValidationResponse> authorizeTheRequest(
			@RequestHeader(value = "Authorization", required = true) String requestTokenHeader) {
		log.debug("Inside authorize =============="+requestTokenHeader);
		String jwtToken = null;
		String userName = null;
		boolean isValid = false;
		
		if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
			jwtToken = requestTokenHeader.substring(7);
			log.debug("JWT Token ======================="+jwtToken);
			
			try {
				userName = jwtTokenUtil.getUsernameFromToken(jwtToken);
			} catch (IllegalArgumentException | ExpiredJwtException e) {
				log.error("Token is Not Valid/Expired");
				 isValid = false;
			}
			
			if (userName != null) {
				 isValid = true;
			}
		}
		
		return  ResponseEntity.ok(new TokenValidationResponse(isValid));

	}


}