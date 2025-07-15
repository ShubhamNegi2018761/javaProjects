package com.fitness.activityservice.service;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.model.ActivityType;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final UserValidationService userValidationService;

    @Autowired
    private  ActivityRepository activityRepository;

    private final RabbitTemplate rabbitTemplate;

    //rabbit mq
    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    public ActivityResponse trackActivity(ActivityRequest request){

        boolean isValid=userValidationService.validateUser(request.getUserId());

        if(!isValid){
            throw new RuntimeException("Invalid User : "+request.getUserId());
        }
        
        Activity activity=Activity.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .duration(request.getDuration())
                .caloriesBurned(request.getCaloriesBurned())
                .startTime(request.getStartTime())
                .additionalMetrics(request.getAdditionalMetrics())
                .build();


         Activity savedActivity=activityRepository.save(activity);

         //publish to rabbitMQ for AI Processing
         //asyncronous communication for message communication

        try{
            rabbitTemplate.convertAndSend(exchange,routingKey,savedActivity);
        } catch (Exception e) {
            log.error("Failed to publish activity to RabbitMq : "+e);
        }
         return mapToResponse(savedActivity);
    }

    private ActivityResponse mapToResponse(Activity activity){

          ActivityResponse res=new ActivityResponse();

          res.setId(activity.getId());
          res.setType(activity.getType());
          res.setUserId(activity.getUserId());
          res.setDuration(activity.getDuration());
          res.setCaloriesBurned(activity.getCaloriesBurned());
          res.setStartTime(activity.getStartTime());
          res.setAdditionalMetrics(activity.getAdditionalMetrics());
          res.setCreatedAt(activity.getCreatedAt());
          res.setUpdatedAt(activity.getUpdatedAt());

          return res;
    }


    public List<ActivityResponse> getUserActivities(String userId) {
          List<Activity> activities= activityRepository.findByUserId(userId);

          //stream api use of map

        return activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ActivityResponse getActivity(String activityId) {

        return  activityRepository
                .findById(activityId)
                .map(this::mapToResponse)
                .orElseThrow(()-> new RuntimeException("Activity not found with Id : "+activityId));

    }

    public List<ActivityResponse> getUsers() {

        List<Activity>activities=activityRepository.findAll();

        return activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}
