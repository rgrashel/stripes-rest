/*
 * Copyright 2014 Rick Grashel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.stripesrest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.stripes.action.ErrorResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.ExecutionContext;
import net.sourceforge.stripes.controller.Interceptor;
import net.sourceforge.stripes.controller.Intercepts;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.controller.StripesConstants;
import net.sourceforge.stripes.util.Log;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;

/**
 * This interceptor is responsible for ensuring that the proper event handler
 * methods are called for Stripes REST action beans.
 */
@Intercepts(
        {
            LifecycleStage.HandlerResolution, LifecycleStage.BindingAndValidation, LifecycleStage.CustomValidation
        })
public class RestActionInterceptor implements Interceptor
{

    private static final Log log = Log.getInstance(RestActionInterceptor.class);

    /**
     * Intercepts execution and checks that the user has appropriate
     * permissions.
     *
     * @param ctx - Execution context
     * @return Resolution to next step in lifecycle
     * @throws Exception if something goes wrong
     */
    public Resolution intercept( ExecutionContext ctx ) throws Exception
    {
        if ( RestActionBean.class.isAssignableFrom(ctx.getActionBean().getClass()) )
        {
            log.debug("Found Rest API Action Bean: ", ctx.getActionBean().getClass());

            // Perform the REST handler resolution before handler resolution occurs
            if ( ctx.getLifecycleStage() == LifecycleStage.HandlerResolution )
            {
                log.debug("Found Rest API Action Bean: ", ctx.getActionBean().getClass());

                // Get the http method
                String httpMethod = ctx.getActionBeanContext().getRequest().getMethod().toLowerCase();

                log.debug("(" + ctx.getActionBean().getClass(), ") HTTP method : ", httpMethod);

                // See if the event handler for the HTTP method exists in the target REST action bean
                try
                {
                    ctx.getActionBean().getClass().getMethod(httpMethod);
                    log.debug("(", ctx.getActionBean().getClass(), ") HTTP method successfully found for : ", httpMethod);
                }
                catch ( NoSuchMethodException nsme )
                {
                    log.error("(" + ctx.getActionBean().getClass() + ") No HTTP method found for : ", httpMethod);
                    return new ErrorResolution(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "This resource does not support the HTTP method : " + httpMethod.toUpperCase());
                }

                // Override the Stripes event with the HTTP method/verb
                ctx.getActionBeanContext().getRequest().setAttribute(StripesConstants.REQ_ATTR_EVENT_NAME, httpMethod);

                return ctx.proceed();

                // Nothing to process after hander resolution
            }

            // After the binding and validation lifecycle stages, check for resource not found errors due to type conversion.
            if ( ctx.getLifecycleStage() == LifecycleStage.BindingAndValidation )
            {
                // Do nothing before binding and validation
                ctx.proceed();

                log.debug("(", ctx.getActionBean().getClass(), ") Checking for Resource Not Found errors after : ", ctx.getLifecycleStage().name());

                // Check for Resource Not Found Errors.  If any exist, return 
                // the 404.
                ValidationErrors validationErrors = ctx.getActionBeanContext().getValidationErrors();

                for ( List< ValidationError> validationErrorList : validationErrors.values() )
                {
                    for ( ValidationError error : validationErrorList )
                    {
                        if ( ResourceNotFoundError.class.isAssignableFrom(error.getClass()) )
                        {
                            return new ErrorResolution(HttpServletResponse.SC_NOT_FOUND, error.getMessage(null));
                        }
                    }
                }

            }

            // After the validation lifecycle stages, check for errors.  If any exist, then convert to an Error Resolution and return that
            if ( ctx.getLifecycleStage() == LifecycleStage.CustomValidation )
            {
                // Nothing to process before validation occurs
                Resolution finalResolution = ctx.proceed();

                // Now check for validation errors.  If any exist, then return the error resolution
                ValidationErrors validationErrors = ctx.getActionBeanContext().getValidationErrors();

                log.debug("(", ctx.getActionBean().getClass(), ") Checking for validation errors : ", ctx.getLifecycleStage().name());

                if ( validationErrors != null && !validationErrors.isEmpty() )
                {
                    log.debug("(", ctx.getActionBean().getClass(), ") Found validation errors : ", ctx.getLifecycleStage().name());

                    Map< Object, Object> jsonErrorMap = new HashMap< Object, Object>();

                    // First, append the global errors -- if any
                    List< String> jsonGlobalErrors = new ArrayList< String>();

                    if ( !validationErrors.hasFieldErrors() )
                    {
                        log.debug("(", ctx.getActionBean().getClass(), ") Found global errors : ", ctx.getLifecycleStage().name());

                        List< ValidationError> globalErrors = validationErrors.get(ValidationErrors.GLOBAL_ERROR);

                        for ( ValidationError validationError : globalErrors )
                        {
                            jsonGlobalErrors.add(validationError.getMessage(null));
                        }

                    }

                    jsonErrorMap.put("globalErrors", jsonGlobalErrors);

                    ArrayList< Map< String, Object>> allFieldErrors = new ArrayList< Map< String, Object>>();

                    // Next, append the field errors -- if any
                    if ( validationErrors.hasFieldErrors() )
                    {
                        log.debug("(", ctx.getActionBean().getClass(), ") Found field errors : ", ctx.getLifecycleStage().name());

                        for ( String fieldName : validationErrors.keySet() )
                        {
                            if ( !fieldName.equals(ValidationErrors.GLOBAL_ERROR) )
                            {
                                List< ValidationError> fieldValidationErrors = validationErrors.get(fieldName);
                                Map< String, Object> fieldErrors = new HashMap< String, Object>();
                                fieldErrors.put("fieldName", fieldName);
                                fieldErrors.put("fieldValue", fieldValidationErrors.get(0).getFieldValue());

                                List< String> fieldErrorMessages = new ArrayList< String>();
                                for ( ValidationError validationError : fieldValidationErrors )
                                {
                                    fieldErrorMessages.add(validationError.getMessage(null));
                                }

                                fieldErrors.put("errorMessages", fieldErrorMessages);

                                allFieldErrors.add(fieldErrors);
                            }
                        }
                        
                        jsonErrorMap.put("fieldErrors", allFieldErrors);
                    }

                    JsonBuilder jsonBuilder = new JsonBuilder(jsonErrorMap);

                    log.debug("(", ctx.getActionBean().getClass(), ") Returning validation error resolution : ", ctx.getLifecycleStage().name());

                    return new ErrorResolution(HttpServletResponse.SC_BAD_REQUEST, jsonBuilder.build());
                }
                else
                {
                    return finalResolution;
                }
            }
        }

        return ctx.proceed();
    }

}
