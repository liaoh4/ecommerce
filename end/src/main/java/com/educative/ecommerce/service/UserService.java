package com.educative.ecommerce.service;

import com.educative.ecommerce.config.MessageStrings;
import com.educative.ecommerce.dto.user.SignInResponseDto;
import com.educative.ecommerce.dto.user.SignUpResponseDto;
import com.educative.ecommerce.dto.user.SignupDto;
import com.educative.ecommerce.dto.user.SignInDto;
import com.educative.ecommerce.exceptions.AuthenticationFailException;
import com.educative.ecommerce.exceptions.CustomException;
import com.educative.ecommerce.model.AuthenticationToken;
import com.educative.ecommerce.model.User;
import com.educative.ecommerce.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;


@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    AuthenticationService authenticationService;
    Logger logger = LoggerFactory.getLogger(UserService.class);

    public SignUpResponseDto signUp(SignupDto signupDto) throws CustomException {

        if (Objects.nonNull(userRepository.findByEmail(signupDto.getEmail()))) {
            throw new CustomException("User already exists");
        }
        String encryptedPassword = signupDto.getPassword();
        try {
            encryptedPassword = hashPassword(signupDto.getPassword());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            logger.error("hashing password failed {}", e.getMessage());
        }
        User user = new User(signupDto.getFirstName(), signupDto.getLastName(), signupDto.getEmail(), encryptedPassword);
        try {
            userRepository.save(user);
            final AuthenticationToken authenticationToken = new AuthenticationToken(user);
            authenticationService.saveConfirmationToken(authenticationToken);
            return new SignUpResponseDto("SUCCESS", "user created successfully");
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    public SignInResponseDto signIn(SignInDto signInDto) throws AuthenticationFailException, CustomException {
        // first find User by email
        User user = userRepository.findByEmail(signInDto.getEmail());
        if(!Objects.nonNull(user)){
            throw new AuthenticationFailException("user not present");
        }
        try {
            // check if password is right
            if (!user.getPassword().equals(hashPassword(signInDto.getPassword()))){
                // passwords do not match
                throw  new AuthenticationFailException(MessageStrings.WRONG_PASSWORD);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            logger.error("hashing password failed {}", e.getMessage());
            throw new CustomException(e.getMessage());
        }

        AuthenticationToken token = authenticationService.getToken(user);

        if(!Objects.nonNull(token)) {
            // token not present
            throw new CustomException(MessageStrings.AUTH_TOEKN_NOT_PRESENT);
        }

        return new SignInResponseDto("success", token.getToken());
    }

    String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes());
        byte[] digest = md.digest();
        String myHash = DatatypeConverter
                .printHexBinary(digest).toUpperCase();
        return myHash;

    }
}
