package com.tmsapp.tms.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tmsapp.tms.Entity.Accgroup;
import com.tmsapp.tms.Entity.Account;
import com.tmsapp.tms.Entity.Application;
import com.tmsapp.tms.Repository.AccgroupRepository;
import com.tmsapp.tms.Repository.AccountRepository;
import com.tmsapp.tms.Repository.ApplicationRepository;

@Service
public class ApplicationService {
    
    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    AccgroupRepository accgroupRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    Checkgroup checkgroup;

    public Map<String, Object> createApplication(Map<String, Object> req){
        Map<String, Object> result = new HashMap<>(); 
        //Check if user is in Project leader group 
        if(req.get("un") == null || req.get("gn") == null){
            result.put("success", false);
            result.put("message", "no un gn");
            return result;
        }
        Account account = accountRepository.getAccountByUsername(req.get("un").toString());
        if(account.getStatus() == 0) {
            result.put("success", false);
            result.put("message", "user inactive");
            return result;
        }


        boolean isPL =  checkgroup.checkgroup(req.get("un").toString(), req.get("gn").toString());
        if(!isPL){
            result.put("success", false);
            result.put("message", "not pl");
            return result; 
        }

        //Check for mandatory fields
        if(req.get("acronym") == null || req.get("rnumber") == null || req.get("startDate") == null || req.get("endDate") == null){
            result.put("success", false);
            result.put("message", "missing madatory fields");
            return result;
        }

        //Check date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        LocalDate checkStartDate = null;
        LocalDate checkEndDate = null;

        try{
            checkStartDate = LocalDate.parse(req.get("startDate").toString());
            checkEndDate = LocalDate.parse(req.get("endDate").toString());
            if(checkStartDate != null && checkEndDate != null){
                int compareDateResult = checkStartDate.compareTo(checkEndDate);
                if(compareDateResult > 0){
                    result.put("success", false);
                    result.put("message", "start date cannot be after the end date");
                    return result;
                }
            }
        }catch(Exception e){
            result.put("success", false);
            result.put("message", "invalid date format");
            return result;
        }

        //Validate permit groups
        String open = null;
        String toDo = null;
        String doing = null;
        String done = null;
        String create = null;
        String description = null;
        if(req.get("open") != null){
            Accgroup tempgroup = accgroupRepository.getGroupByGroupName(req.get("open").toString());
            if(tempgroup != null){
                open = tempgroup.getGroupName();
            }
            else{
                result.put("success", false);
                result.put("message", "invalid group");
                return result;
            }
        }
        if(req.get("toDo") != null){
            Accgroup tempgroup = accgroupRepository.getGroupByGroupName(req.get("toDo").toString());
            if(tempgroup != null){
                toDo = tempgroup.getGroupName();
            }
            else{
                result.put("success", false);
                result.put("message", "invalid group");
                return result;
            }
        }
        if(req.get("doing") != null){
            Accgroup tempgroup = accgroupRepository.getGroupByGroupName(req.get("doing").toString());
            if(tempgroup != null){
                doing = tempgroup.getGroupName();
            }
            else{
                result.put("success", false);
                result.put("message", "invalid group");
                return result;
            }
        }
        if(req.get("done") != null){
            Accgroup tempgroup = accgroupRepository.getGroupByGroupName(req.get("done").toString());
            if(tempgroup != null){
                done = tempgroup.getGroupName();
            }
            else{
                result.put("success", false);
                result.put("message", "invalid group");
                return result;
            }
        }
        if(req.get("create") != null){
            Accgroup tempgroup = accgroupRepository.getGroupByGroupName(req.get("create").toString());
            if(tempgroup != null){
                create = tempgroup.getGroupName();
            }
            else{
                result.put("success", false);
                result.put("message", "invalid group");
                return result;
            }
        }
        if (req.get("description") != null){
            description = req.get("description").toString();
        }

        //Check r_number
        String rNumberRegex = "^\\d+$";
        if(!req.get("rnumber").toString().matches(rNumberRegex)){
            result.put("success", false);
            result.put("message", "incorrect rnumber");
            return result;
        }

        // System.out.println("description: " + req.get("description").toString());
        //Construct application
        Application application = new Application(req.get("acronym").toString(), description, Integer.valueOf(req.get("rnumber").toString()), checkStartDate, checkEndDate, create, open, toDo, doing, done);
        Map<String, Object> isCreated = applicationRepository.createApplication(application);
        if((Boolean) isCreated.get("success")){
            result.put("success", true);
        }
        else if(isCreated.get("message").toString().equals("application exists")) {
            result.put("message", "application exists");
            result.put("success", false);
        }
        else{
            result.put("message", "not created for some reason");
            result.put("success", false);
        }

        return result;
    }

    public Map<String, Object> getApplication(Map<String, Object> req){
        Map<String, Object> result = new HashMap<>(); 
        if(req.get("appAcronym") == null){
            result.put("success", false);
            return result;
        }
        try {
            Application application = applicationRepository.getApplication(req.get("appAcronym").toString());

            if(application == null) {
                result.put("success", false);
            } else {
                result.put("success", true);
                result.put("application", application);
            }
            return result;
        }
        catch(Exception e) {
            result.put("success", false);
            result.put("error", e.getStackTrace());
        }
        

        return result;
    }

    public Map<String, Object> updateApplication(Map<String, Object> req) throws ParseException{
        Map<String, Object> result = new HashMap<>(); 
        //Check if user is in Project leader group 
        
        if(req.get("un") == null || req.get("gn") == null){
            result.put("success", false);
            result.put("message", "no un gn");
            return result;
        }
        Account account = accountRepository.getAccountByUsername((req.get("un").toString()));
        if(account.getStatus() == 0) {
            result.put("message", "user inactive");
            result.put("success", false);
            return result; 
        }
        boolean isPL =  checkgroup.checkgroup(req.get("un").toString(), req.get("gn").toString());
        if(!isPL){ 
            result.put("message", "not pl");
            result.put("success", false);
            return result; 
        }
        System.out.println(" application after pl  " + req.get("acronym").toString());
        //Retrieve application 
        Application application = applicationRepository.getApplication(req.get("acronym").toString());

        if(application == null){
            result.put("success", false);
            result.put("message", "no application found");
            return result;
        }
        //update application changes
        if(req.get("endDate") != null){
            String dateString = req.get("endDate").toString();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            LocalDate temp = LocalDate.parse(dateString);

            // LocalDate temp = (LocalDate) req.get("endDate");
            System.out.println("temp " + temp);
            LocalDate tempStartDate = application.getApp_startDate();
            int dateCompare = tempStartDate.compareTo(temp);
            //Start date is after end date
            if(dateCompare > 0 ){
                result.put("success", false);
                result.put("message", "end date cannot be earlier than start date");
                return result;
            }
            if(temp != null){
                System.out.println("temp " + temp);
                application.setApp_endDate(temp);
            }
        }
        if(req.get("permitCreate") != null){
            System.out.println(req.get("permitCreate"));
            Accgroup temp = accgroupRepository.getGroupByGroupName(req.get("permitCreate").toString());
            if(temp != null){
                application.setApp_permit_Create(temp.getGroupName());
            }
        }
        if(req.get("permitOpen") != null){
            Accgroup temp = accgroupRepository.getGroupByGroupName(req.get("permitOpen").toString());
            if(temp != null){
                application.setApp_permit_Open(temp.getGroupName());
            }
        }
        if(req.get("permitTodo") != null){
            Accgroup temp = accgroupRepository.getGroupByGroupName(req.get("permitTodo").toString());
            if(temp != null){
                application.setApp_permit_toDoList(temp.getGroupName());
            }
        }
        if(req.get("permitDoing") != null){
            Accgroup temp = accgroupRepository.getGroupByGroupName(req.get("permitDoing").toString());
            if(temp != null){
                application.setApp_permit_Doing(temp.getGroupName());
            }
        }
        if(req.get("permitDone") != null){
            Accgroup temp = accgroupRepository.getGroupByGroupName(req.get("permitDone").toString());
            if(temp != null){
                application.setApp_permit_Done(temp.getGroupName());
            }
        }
        if(req.get("description") != null) {
            application.setApp_Description(req.get("description").toString());
        }
        // if(req.get("rnumber") != null){
        //     int temp = (int) req.get("rnumber");
        //     application.setApp_Rnumber(temp);
        // }
        System.out.println(application);
        //Update application 
        boolean isUpdated = applicationRepository.updateApplication(application);
        if(isUpdated) result.put("success", true);
        else {
            result.put("success", false);
            result.put("message", "application failed to update");
        }
        return result;
    }

    public Map<String, Object> getAllApplication(){
        Map<String, Object> result = new HashMap<>();
        List<Application> applications = applicationRepository.getAllApplication();
        result.put("applications", applications);
        result.put("success",true);
        return result;
    } 
}
