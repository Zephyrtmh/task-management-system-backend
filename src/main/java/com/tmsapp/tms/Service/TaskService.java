package com.tmsapp.tms.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmsapp.tms.Entity.Account;
import com.tmsapp.tms.Entity.Application;
import com.tmsapp.tms.Entity.Plan;
import com.tmsapp.tms.Entity.Task;
import com.tmsapp.tms.Entity.TaskDTO;
import com.tmsapp.tms.Repository.AccountRepository;
import com.tmsapp.tms.Repository.ApplicationRepository;
import com.tmsapp.tms.Repository.PlanRepository;
import com.tmsapp.tms.Repository.TaskRepository;
import com.tmsapp.tms.Util.ApplicationConstant;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private Checkgroup checkGroup;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    EmailService emailService;

    public Map<String, Object> createTask(TaskDTO task, String jwtToken) {
        Map<String, Object> response = new HashMap<>();
        
        //Input validation
        if(task.getTaskName() == null || task.getTaskAppAcronym() == null || task.getTaskCreator() == null || task.getTaskOwner() == null){
            //throw new Error("Missing mandatory field");
            response.put("success", false);
            response.put("message", "Missing mandatory field");
            return response;
        }
        Map<String, Object> appAcronymObj = new HashMap<>();
        appAcronymObj.put("appAcronym", task.getTaskAppAcronym());

        Map<String, Object> applicationObj = applicationService.getApplication(appAcronymObj);

        Application application = new Application();

        if((boolean) applicationObj.get("success") == false) {
            response.put("success", false);
            response.put("message", "Invalid app acronym");
            return response;
        }
        try {
            application = objectMapper.readValue(objectMapper.writeValueAsString(applicationObj.get("application")), Application.class);
        }
        catch(Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
        
        try {
            Jws<Claims> jwtContents = Jwts.parserBuilder()
                .setSigningKey(ApplicationConstant.SECURITY_KEY.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(jwtToken);

                System.out.println(jwtContents);
                String username = jwtContents.getBody().getSubject();
                System.out.println("username: " + username);
                System.out.println("username: " + application.getApp_permit_Create());

                boolean authorized = checkGroup.checkgroup(username, application.getApp_permit_Create());
                System.out.println(authorized);
                Account account = accountRepository.getAccountByUsername(username);
                if(account.getStatus() == 0) {
                    response.put("success", false);
                    response.put("message", "user inactive");
                    return response;
                }
                if(!authorized) {
                    response.put("success", false);
                    response.put("message", "User does not have permission");
                    return response;
                }

            // Account user =  accountRepository.getAccountByUsername(username);
        }
        catch(Exception e) {
            response.put("success", false);
            response.put("message", "User does not have permission");
            return response;
        }

        //Get & check fields 
        String taskName, taskDescription, taskNotes, taskId, taskPlan, taskApp, taskState, taskCreator, taskOwner, createDate;
        
        // Application application = applicationRepository.getApplication(task.getTaskAppAcronym());
        String appAcronym =application.getApp_Acronym();

        Plan plan = new Plan();

        try {
            plan = planRepository.getPlansByPlanName(task.getTaskPlan());
        }
        catch(Exception e) {
            plan = null;
        }

        

        // System generate task note
        // LocalDate tempNow = LocalDate.now();
        Date tempNow = new Date();
        String systemNotes = "system|open|" + tempNow.toInstant().toString() + "|Task created";
        taskNotes = systemNotes;
        System.out.println(task.getTaskNotes());
        if (task.getTaskNotes() != null && !task.getTaskNotes().equals("")) {
            String notesRegex = "\\|";
            if (!task.getTaskNotes().matches(notesRegex)) {
                System.out.println("user notes present");
                String userNotes = "||" +  task.getTaskCreator() + "|open|" + tempNow.toInstant().toString() + "|" + task.getTaskNotes();
                System.out.println(userNotes);
                taskNotes += userNotes;
                // System.out.println(taskNotes);
            }
        }
        System.out.println(taskNotes);
        task.setTaskNotes(taskNotes);

        //system generate taskId from application Rnumber
        Integer appRNumber = applicationRepository.getApplicationRNumber(appAcronym);
        taskId = appAcronym+"_"+ String.valueOf(appRNumber);
        task.setTaskId(taskId);
        //update appRNumber;
        application.setApp_Rnumber(appRNumber + 1);
        applicationRepository.updateApplication(application);

        //create date
        task.setTaskCreateDate(tempNow);
        if(task.getTaskState() == null) {
            task.setTaskState("open");
        } 

        try {
            System.out.println("applicsation in service: " + application);
            System.out.println("plan in service: " + plan);
            taskRepository.createTask(new Task(task, application, plan));
            response.put("success", true);
            return response;
        }
        catch(Error err) {
            System.err.println(err);
            response.put("success", false);
            response.put("message", "Failed to create Task");
            return response;
        }
    }

    
    public TaskDTO getTaskById(String taskId) {
        TaskDTO task = new TaskDTO();
        try {
            System.out.println("task id task service " + taskId);
            task = taskRepository.getTaskById(taskId);
        }
        catch (Error err) {
            System.err.println(err);
        }

        return task;
    }

    public List<TaskDTO> getTasksByPlan(String taskPlan) {
        List<TaskDTO> tasksDTO = new ArrayList<>();
        try {
            tasksDTO = taskRepository.getTasksByPlan(taskPlan);
        }
        catch (Error err) {
            System.err.println(err);
        }

        return tasksDTO;
    }

    public List<TaskDTO> getTasksByApplication(String appAcronym) {
        List<TaskDTO> tasksDTO = new ArrayList<>();
        try {
            tasksDTO = taskRepository.getTasksByApplication(appAcronym);
        }
        catch (Error err) {
            System.err.println(err);
        }

        return tasksDTO;
    }

    @Transactional
    public List<TaskDTO> getAllTask() {
        List<TaskDTO> taskList = new ArrayList<TaskDTO>();

        try {
            taskList = taskRepository.getAllTask();
        }
        catch (Error err) {
            System.err.println(err);
        }

        return taskList;
    }

    public Map<String, Object> PMEditTask (Map<String, Object> req){
        Map<String, Object> response = new HashMap<>();
        String id,un,state;
        //Check for required fields 
        // System.out.println(req.get("taskId").toString());
        // System.out.println(req.get("taskState").toString());
        if(req.get("taskId") == null || req.get("un") == null || req.get("taskState") == null){
            response.put("success", false);
            response.put("message", "mandatory fields missing");
            return response;
        } else{
            id = req.get("taskId").toString();
            un = req.get("un").toString().toLowerCase();
            state = req.get("taskState").toString().toLowerCase();
            if (!state.equals("open") && !state.equals("todo")){
                response.put("success", false);
                response.put("message", "state input invalid");
                return response;
            }
        }

        //Get application and Task
        Application application;
        Plan plan;
        TaskDTO task = taskRepository.getTaskById(id);

        if(task == null){
            response.put("success", false);
            response.put("message", "Invalid task id");
            return response;
        } else{
            //task state not in open    
            if(!task.getTaskState().toLowerCase().equals("open")){
                response.put("success", false);
                response.put("message", "current task is not in open state");
                return response;
            }
            application = applicationRepository.getApplication(task.getTaskAppAcronym());
            if(application !=null){ 
                //check if user is pm 
                boolean isPM = checkGroup.checkgroup(un,application.getApp_permit_Open());
                if(!isPM){
                    response.put("success", false);
                    response.put("message", "unauthorized (NOT PM)");
                    return response;
                }
            }else{
                response.put("success", false);
                response.put("message", "application not found");
                return response;
            }
        }

        //Get current date
        // LocalDate date = LocalDate.now();
        Date date = new Date();
        //System/User's notes
        String notes="";

        //Check if plan exisit if req.get("taskPlan") != null
        if(req.get("taskPlan") !=null){
            plan = planRepository.getPlansByPlanName(req.get("taskPlan").toString());
            if(plan == null){
                response.put("success", false);
                response.put("message", "Invalid plan name");
                return response;
            }
            String oldplan = "";
            if (task.getTaskPlan() != null){
                oldplan = task.getTaskPlan();
            }     
            if (!oldplan.equals(plan.getPlan_MVP_name())){
                task.setTaskPlan(plan.getPlan_MVP_name());
                notes += "||system|" + task.getTaskState() + "|" + date.toInstant().toString() + "| Task plan changed to " + plan.getPlan_MVP_name();
            }
        }else{
            plan = null;
            if (task.getTaskPlan() != null){
                notes += "||system|" + task.getTaskState() + "|" + date.toInstant().toString() + "| Task plan removed";
            }
        }

        //there is new notes
        if(req.get("userNotes") != null){
            notes += "||" + req.get("un").toString() + "|" + task.getTaskState() + "|" + date.toInstant().toString()+ "|" + req.get("userNotes");
        }
       
        //task is being promoted
        if(state.equals("todo")){
            notes += "||system|" + task.getTaskState() + ">" + state + "|" + date.toInstant().toString() + "| Updated task state";
            task.setTaskState(state);
        }

        if(!notes.equals("")){
            task.setTaskNotes(task.getTaskNotes().concat(notes));
        }

        //Update task creator
        task.setTaskOwner(un);

        //update task
        Task updateTask = new Task(task, application, plan);
        boolean isUpdated = taskRepository.updateTask(updateTask);
        //Return
        if(isUpdated){
            response.put("success", true);
            return response;
        }

        response.put("success", false);
        return response;
    }

    public Map<String, Object> PLEditTask (Map<String, Object> req){
        Map<String, Object> response = new HashMap<>();
        //Check for required fields 
        if(req.get("taskId") == null || req.get("un") == null || req.get("taskState") == null){//} || req.get("acronym") == null){
            response.put("success", false);
            response.put("message", "mandatory fields missing");
            return response;
        }

        //Get application and Task
        // Application application = applicationRepository.getApplication(req.get("acronym").toString());
        TaskDTO task = taskRepository.getTaskById(req.get("taskId").toString());
        
        if(task == null){
            response.put("success", false);
            response.put("message", "invalid TaskId");
            return response;
        }
        Application application = applicationRepository.getApplication(task.getTaskAppAcronym());
        Plan newPlan = null;

        //check if user has app_permit_create role
        boolean isPL = checkGroup.checkgroup(req.get("un").toString(), application.getApp_permit_Create());
        if(!isPL){
            response.put("success", false);
            response.put("message", "unauthorized (NOT PM)");
            return response;  
        }

        //Check if plan exisit if req.get("taskPlan") != null
        //Get current date
        // LocalDate tempDateNow = LocalDate.now();

        Date tempDateNow = new Date();

        //System/User's notes
        String notes="";

        if(req.get("taskPlan") !=null){
            newPlan = planRepository.getPlansByPlanName(req.get("taskPlan").toString());
            if(newPlan == null){
                response.put("success", false);
                response.put("message", "invalid plan name");
                return response;
            }
            String oldplan = "";
            if (task.getTaskPlan() != null){
                oldplan = task.getTaskPlan();
            }   
            if (!oldplan.equals(newPlan.getPlan_MVP_name())){
                task.setTaskPlan(newPlan.getPlan_MVP_name());
                notes += "||system|" + task.getTaskState() + "|" + tempDateNow.toInstant().toString() + "| Task plan changed to " + newPlan.getPlan_MVP_name();
            }
        }else{
            newPlan = null;
            if (task.getTaskPlan() != null){
                notes += "||system|" + task.getTaskState() + "|" + tempDateNow.toInstant().toString() + "| Task plan removed";
            }
        }

        
        //task state 
        //Check current task state as done
        System.out.println(task.getTaskState());
        if(!task.getTaskState().toLowerCase().equals("done")){
            response.put("success", false);
            response.put("message", "current task is not in done state");
            return response;
        }
        
        if(req.get("userNotes") != null){
            notes += "||" + req.get("un").toString() + "|" + task.getTaskState().toLowerCase() + "|" + tempDateNow.toInstant().toString() + "|" + req.get("userNotes");
        }

        if(!(task.getTaskState().toLowerCase().equals(req.get("taskState").toString().toLowerCase()))){
            
            if(req.get("taskState").toString().equals("doing") || req.get("taskState").toString().equals("done") || req.get("taskState").toString().equals("closed")){
                //do nothing
            }else{
                response.put("success", false);
                response.put("message", "input task state incorrect");
                return response;
            }
            notes += "||system|" + task.getTaskState() +">"+ req.get("taskState").toString()+ "|" + tempDateNow.toInstant().toString() + "| Updated task state";
            task.setTaskState(req.get("taskState").toString());
        }
        
        if(notes != ""){
            task.setTaskNotes(task.getTaskNotes().concat(notes));
        }

        //Update task creator
        task.setTaskOwner(req.get("un").toString());

        //update task
        Task updateTask = new Task(task, application, newPlan);
        boolean isUpdated = taskRepository.updateTask(updateTask);
        
        //Return
        if(isUpdated){
            response.put("success", true);
            return response;
        }

        response.put("success", false);
        response.put("message", "task update error");
        return response;
    }

    @Transactional
    public Map<String, Object> TMEditTask (Map<String, Object> req){
        Map<String, Object> response = new HashMap<>();

        //Check for required fields 
        if(req.get("taskId") == null || req.get("un") == null || req.get("gn") == null || req.get("taskState") == null || req.get("acronym")==null){
            response.put("success", false);
            response.put("message", "mandatory fields missing");
            return response;
        }
        String newtaskstate = req.get("taskState").toString().toLowerCase();

        //check if user is pm 
        boolean isTM = checkGroup.checkgroup(req.get("un").toString(), req.get("gn").toString());
        if(!isTM){
            response.put("success", false);
            response.put("message", "unauthorized (NOT TM)");
            return response;  
        }

        String id = req.get("taskId").toString();

        Application application;
        // Plan newPlan;
        TaskDTO task = taskRepository.getTaskById(id);


        if(task == null){
            response.put("success", false);
            response.put("message", "Invalid task id");
            return response;
        } else{
            //taskid found  
            //task state not in todo or doing
            if(!task.getTaskState().toLowerCase().equals("todo") && !task.getTaskState().toLowerCase().equals("doing")){
                System.out.println("task.getTaskState() " + task.getTaskState());
                response.put("success", false);
                response.put("message", "invalid task state");
                return response;
            }
            //task
            application = applicationRepository.getApplication(task.getTaskAppAcronym());
        }

        //Get current date
        // LocalDate tempDateNow = LocalDate.now();
        Date tempDateNow = new Date();

        //System/User's notes
        String notes = "";
        //task state 
        //Check current task state as open
        System.out.println("task.getTaskState()" +task.getTaskState());
        System.out.println("req.get(\"taskState\") "  + req.get("taskState"));

        if(req.get("userNotes") != null){
           notes += "||"+req.get("un").toString() + "|" + task.getTaskState() + "|" + tempDateNow.toInstant().toString() + "|" + req.get("userNotes").toString();
        }
        if(!task.getTaskState().toLowerCase().equals(req.get("taskState").toString().toLowerCase())){
            if (!((task.getTaskState().toLowerCase().equals("todo") && newtaskstate.equals("doing")) || (task.getTaskState().toLowerCase().equals("doing") && (newtaskstate.equals("done") || newtaskstate.equals("todo"))))){
                response.put("success", false);
                response.put("message", "invalid task state update");
                return response;
            }

            notes += "||system|" + task.getTaskState()+">"+ req.get("taskState").toString()+ "|" + tempDateNow.toInstant().toString() + "| Updated task state";
            task.setTaskState(req.get("taskState").toString());
        }

        if(notes != ""){
            task.setTaskNotes(task.getTaskNotes().concat(notes));
        }

        //Update task creator
        task.setTaskOwner(req.get("un").toString());
        
        Plan newPlan = planRepository.getPlansByPlanName(task.getTaskPlan());

        //update task
        Task updateTask = new Task(task, application, newPlan);

        boolean isUpdated = taskRepository.updateTask(updateTask);

        // String username = req.get("un").toString();
        // System.out.println("username " + username);
        // String email = accountRepository.getEmail(username);
        // System.out.println("email " + email);

        // System.out.println("true " + (req.get("taskState").equals("done") && isUpdated));

        // if(req.get("taskState").equals("done") && isUpdated){
        //     emailService.sendEmail(email, "Promote task " + req.get("taskId").toString() +" to done", "Promote task " + req.get("taskId").toString() +" to done");
        // }
        
        //Return
        if(isUpdated){
            response.put("success", true);
            return response;
        }

        response.put("success", false);
        response.put("message", "task update error");
        return response;
    }



    //email 

    public Map<String, Object> Email (Map<String, Object> req){
        Map<String, Object> response = new HashMap<>();

        //Check for required fields 
        if(req.get("taskId") == null || req.get("un") == null || req.get("gn") == null){
            response.put("success", false);
            response.put("message", "mandatory fields missing");
            return response;
        }

        //check if user is pm 
        boolean isTM = checkGroup.checkgroup(req.get("un").toString(), req.get("gn").toString());
        if(!isTM){
            response.put("success", false);
            response.put("message", "unauthorized (NOT TM)");
            return response;  
        }

        String id = req.get("taskId").toString();

        Application application;
        // Plan newPlan;
        TaskDTO task = taskRepository.getTaskById(id);


        if(task == null){
            response.put("success", false);
            response.put("message", "invalid task id");
            return response;
        } 
        else{
            //taskid found  
            //task state not in todo or doing
            if(!task.getTaskState().toLowerCase().equals("done")){
                System.out.println("task.getTaskState() " + task.getTaskState());
                response.put("success", false);
                response.put("message", "invalid task state");
                return response;
            }
            //task
            application = applicationRepository.getApplication(task.getTaskAppAcronym());
        }

        String username = req.get("un").toString();
        System.out.println("username " + username);
        String email = accountRepository.getEmail(username);
        System.out.println("email " + email);

        if( email == null || email.equals("")) {
            response.put("success", true);
            response.put("message", "sender's email not found");
        }
        else {
            if(task.getTaskState().toLowerCase().equals("done")) {
                emailService.sendEmail(email, "Promote task " + req.get("taskId").toString() +" to done", "Promote task " + req.get("taskId").toString() +" to done");

                response.put("success", true);
                return response;
            }
        }
        

        response.put("success", false);
        return response;
    }
}
