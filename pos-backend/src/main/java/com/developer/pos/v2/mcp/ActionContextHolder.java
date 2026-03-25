package com.developer.pos.v2.mcp;

import com.developer.pos.v2.mcp.model.ActionContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class ActionContextHolder {
    private ActionContext context = ActionContext.humanDefault();

    public ActionContext getContext() { return context; }
    public void setContext(ActionContext context) { this.context = context; }
}
