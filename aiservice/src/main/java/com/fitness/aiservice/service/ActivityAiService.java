package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAiService {

    private final GemniService gemniService;

    public Recommendation generateRecommendation(Activity activity){
           String prompt=createPromptForActivity(activity);

           String aiResponse=gemniService.getAnswer(prompt);
           log.info("RESPONSE FROM AI : {}",aiResponse);

           return processAiResponse(activity,aiResponse);
    }

    private Recommendation processAiResponse(Activity activity,String aiResponse){
         try {
             ObjectMapper mapper=new ObjectMapper();
             JsonNode rootNode=mapper.readTree(aiResponse);

             JsonNode textNode=rootNode.path("candidates")
                     .get(0)
                     .path("content")
                     .path("parts")
                     .get(0)
                     .path("text");

             String jsonContent=textNode.asText()
                     .replaceAll("```json\\n","")
                     .replaceAll("\\n```","")
                     .trim();

//             log.info("PAST RESPONSE FROM AI : {}",jsonContent);

             JsonNode analysisJson=mapper.readTree(jsonContent);

             JsonNode analysisNode=analysisJson.path("analysis");

             //Analysis

             StringBuilder fullAnalysis=new StringBuilder();
             addAnalysisSection(fullAnalysis,analysisNode,"overall","Overall :");
             addAnalysisSection(fullAnalysis,analysisNode,"pace","Pace :");
             addAnalysisSection(fullAnalysis,analysisNode,"heartRate","Heart Rate :");
             addAnalysisSection(fullAnalysis,analysisNode,"caloriesBurned","Calories :");

             //Improvements
             List<String> improvements=extractImprovements(analysisJson.path("improvements"));


             //Suggestions
             List<String>suggestions=extractSuggestions(analysisJson.path("suggestions"));

             //Safety
             List<String>safety=extractSafetyGuidlines(analysisJson.path("safety"));

             return Recommendation.builder()
                     .activityId(activity.getId())
                     .userId(activity.getUserId())
                     .activityType(activity.getType())
                     .recommendation(fullAnalysis.toString().trim())
                     .improvements(improvements)
                     .suggestions(suggestions)
                     .safety(safety)
                     .createdAt(LocalDateTime.now())
                     .build();


         }catch (Exception e){
             e.printStackTrace();

             return createDefaultRecommendation(activity);
         }




    }

    //default recommendation
    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .recommendation("Unable to Generate detailed analysis")
                .improvements(Collections.singletonList("Continue with your current routine"))
                .suggestions(Collections.singletonList("Consider consulting a fitness proffessional"))
                .safety(Arrays.asList(
                        "Always warm up before Exercise",
                        "Stay hydrated",
                        "Listen to your body"
                ))
                .createdAt(LocalDateTime.now())
                .build();
    }

    //extract safety
    private List<String> extractSafetyGuidlines(JsonNode safetyNode) {
        List<String> safetyList=new ArrayList<>();
        if(safetyNode.isArray()){
            safetyNode.forEach(safety->safetyList.add(safety.asText()));
        }

        return safetyList.isEmpty()?
                Collections.singletonList("Follow General Safety Guidlines"):
                safetyList;

    }


    //extract suggestions
    private List<String> extractSuggestions(JsonNode suggestionsNode) {
        List<String> suggestions=new ArrayList<>();
        if(suggestionsNode.isArray()){
            suggestionsNode.forEach(suggestion->{
                String workout=suggestion.path("workout").asText();
                String description=suggestion.path("description").asText();
                suggestions.add(String.format("%s: %s",workout,description));

            });
        }

        return suggestions.isEmpty()?
                Collections.singletonList("No specific Suggestions provided"):
                suggestions;


    }

    private List<String> extractImprovements(JsonNode improvementsNode) {
        List<String>improvements=new ArrayList<>();
        if(improvementsNode.isArray()){
            improvementsNode.forEach(improvement->{
                String area=improvement.path("area").asText();
                String detail=improvement.path("recommendation").asText();
                improvements.add(String.format("%s: %s",area,detail));

            });
        }

        return improvements.isEmpty()?
                Collections.singletonList("No specific improvements provided"):
                improvements;
    }

    //function for analysis
    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if(!analysisNode.path(key).isMissingNode()){
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");
        }
    }

    private String createPromptForActivity(Activity  activity){
            return String.format("""
                    Analyze this fitness activity and provide detailed recommendations in the following format
                    {
                      "analysis":{
                         "overall":"Overall analysis here",
                         "pace":"Pace analysis here",
                         "heartRate":"Heart rate analysis here",
                         "caloriesBurned":"Calories analysis here"
                      },
                      "improvements":[
                          {
                            "area":"Area name",
                            "recommendation":"Detailed recommendation"
                          }
                      ],
                      "suggestions":[
                           {
                             "workout":"Workout name",
                             "description":"Detailed workout description"
                           }
                       ],
                       "safety":[
                           "Safety point 1",
                           "Safety point 2"
                 
                        ]
                    }
                    
                    Analyze this activity:
                    Activity Type: %s
                    Duration: %d minutes
                    Calories Burned: %d
                    Additional Metrics: %s
                    
                    provide detailde analysis focusing on performance, improvements, next workout suggestions, and safety guidelines.
                    Ensure the response follows the EXACT JSON fromat shown above.
                    """,
                     activity.getType(),
                     activity.getDuration(),
                     activity.getCaloriesBurned(),
                     activity.getAdditionalMetrics()
                    );
    }


}
