package com.fitness.activityservice;

import com.fitness.activityservice.model.Activity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ActivityRepository extends MongoRepository<Activity,String> {

    List<Activity> findByUserId(String userId);

    @Modifying // Indicates that this method modifies the database (e.g., DELETE, UPDATE)
    @Transactional // Ensures the operation runs within a transaction
    boolean deleteAllByUserId(Long userId);


    @Modifying
    @Transactional
    boolean deleteByUserId(Long userId);

}
