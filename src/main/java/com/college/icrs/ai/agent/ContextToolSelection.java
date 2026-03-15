package com.college.icrs.ai.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContextToolSelection implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private NextTool nextTool;
    private String reason;

    public static ContextToolSelection classify() {
        ContextToolSelection selection = new ContextToolSelection();
        selection.setNextTool(NextTool.CLASSIFY);
        selection.setReason("Proceed to classification");
        return selection;
    }

    public ContextToolSelection normalized() {
        ContextToolSelection selection = new ContextToolSelection();
        selection.setNextTool(nextTool != null ? nextTool : NextTool.CLASSIFY);
        selection.setReason(StringUtils.hasText(reason) ? reason.trim() : null);
        return selection;
    }
}
