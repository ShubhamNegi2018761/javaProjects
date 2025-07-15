package com.fitness.userservice.service;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.model.User;
import com.fitness.userservice.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserResponse register(RegisterRequest request) {

           if(userRepository.existsByEmail(request.getEmail())){
               throw new RuntimeException("Email Already exist");
           }

           User user=new User();
           user.setEmail(request.getEmail());
           user.setPassword(request.getPassword());
           user.setFirstName(request.getFirstName());
           user.setLastName(request.getLastName());

           User savedUser=userRepository.save(user);

           UserResponse response=new UserResponse();

           response.setId(savedUser.getId());
           response.setEmail(savedUser.getEmail());
           response.setPassword(savedUser.getPassword());
           response.setFirstName(savedUser.getFirstName());
           response.setLastName(savedUser.getLastName());
           response.setCreatedAt(savedUser.getCreatedAt());
           response.setUpdatedAt(savedUser.getUpdatedAt());

           return  response;
    }

    public UserResponse getUserProfile(String userId) {
        User savedUser=userRepository.findById(userId)
                  .orElseThrow(()->new RuntimeException("User Not Found"));

        UserResponse response=new UserResponse();

        response.setId(savedUser.getId());
        response.setEmail(savedUser.getEmail());
        response.setPassword(savedUser.getPassword());
        response.setFirstName(savedUser.getFirstName());
        response.setLastName(savedUser.getLastName());
        response.setCreatedAt(savedUser.getCreatedAt());
        response.setUpdatedAt(savedUser.getUpdatedAt());

        return  response;

    }

    public Boolean exicstByUserId(String userId) {
           return userRepository.existsById(userId);
    }
}
