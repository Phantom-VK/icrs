package com.college.icrs.ai.knowledge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolutionGuidanceEntry {

    private String officeName;
    private String facultyOrDesk;
    private String responsibility;
    private String building;
    private String floor;
    private String room;
    private String openHours;
    private String contactEmail;
    private String contactPhone;
    private String studentAction;
    private String escalationNote;
}
