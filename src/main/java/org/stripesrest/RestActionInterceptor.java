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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.stripes.action.ErrorResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.controller.ExecutionContext;
import net.sourceforge.stripes.controller.Interceptor;
import net.sourceforge.stripes.controller.Intercepts;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.controller.StripesConstants;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.util.Log;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;

/**
 * This interceptor is responsible for ensuring that the proper event handler
 * methods are called for Stripes REST action beans.
 */
@Intercepts(
                {
            LifecycleStage.HandlerResolution, LifecycleStage.BindingAndValidation, LifecycleStage.CustomValidation, LifecycleStage.EventHandling
        } )
public class RestActionInterceptor implements Interceptor
{

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
        if ( RestActionBean.class.isAssignableFrom( ctx.getActionBean().getClass() ) )
        {
            Log.getInstance( getClass() ).debug( "Found Rest API Action Bean: ", ctx.getActionBean().getClass(), " | Stripes Lifecycle Stage => ", ctx.getLifecycleStage() );

            // Perform the REST handler resolution before handler resolution occurs
            if ( ctx.getLifecycleStage() == LifecycleStage.HandlerResolution )
            {
                Log.getInstance( getClass() ).debug( "Found Rest API Action Bean: ", ctx.getActionBean().getClass() );

                // Get the http method
                String httpMethod = ctx.getActionBeanContext().getRequest().getMethod().toLowerCase();

                // Try to get an event name if one was supplied
                String eventName = StripesFilter.getConfiguration().getActionResolver().getEventName( ctx.getActionBean().getClass(), ctx.getActionBeanContext() );

                Log.getInstance( getClass() ).debug( "(" + ctx.getActionBean().getClass(), ") HTTP method : ", httpMethod, " | Event Name : " + eventName );

                // See if the event handler for the HTTP method exists in the target REST action bean
                try
                {
                    // If we have a passed event name, then we need to check it
                    // to make sure that it is annotated with the HTTP method
                    // being used.
                    if ( eventName != null )
                    {
                        Method method = ctx.getActionBean().getClass().getMethod( eventName );

                        Class annotationClass = Class.forName( "org.stripesrest." + httpMethod.toUpperCase() );
                        boolean eventSupportsHttpMethod = ( method.getDeclaredAnnotation( annotationClass ) != null );

                        if ( !eventSupportsHttpMethod )
                        {
                            return new ErrorResolution( HttpServletResponse.SC_METHOD_NOT_ALLOWED, "The event (" + eventName + ") resource does not support the HTTP method : " + httpMethod.toUpperCase() );
                        }
                    }
                    else
                    {
                        ctx.getActionBean().getClass().getMethod( httpMethod );
                        eventName = httpMethod;
                    }
                    Log.getInstance( getClass() ).debug( "(", ctx.getActionBean().getClass(), ") HTTP method successfully found for : ", httpMethod );
                }
                catch ( NoSuchMethodException nsme )
                {
                    Log.getInstance( getClass() ).error( "(" + ctx.getActionBean().getClass() + ") No HTTP method found for : ", httpMethod );
                    return new ErrorResolution( HttpServletResponse.SC_METHOD_NOT_ALLOWED, "This resource does not support the HTTP method : " + httpMethod.toUpperCase() );
                }

                // Override the Stripes event with the HTTP method/verb
                ctx.getActionBeanContext().getRequest().setAttribute( StripesConstants.REQ_ATTR_EVENT_NAME, eventName );

                return ctx.proceed();

                // Nothing to process after hander resolution
            } 
            else if ( ctx.getLifecycleStage() == LifecycleStage.BindingAndValidation )
            {
                // Do nothing before binding and validation
                Resolution resolution = ctx.proceed();

                Log.getInstance( getClass() ).debug( "(", ctx.getActionBean().getClass(), ") Checking for Resource Not Found errors after : ", ctx.getLifecycleStage().name() );

                // Check for Resource Not Found Errors.  If any exist, return 
                // the 404.
                ValidationErrors validationErrors = ctx.getActionBeanContext().getValidationErrors();

                for ( List< ValidationError> validationErrorList : validationErrors.values() )
                {
                    for ( ValidationError error : validationErrorList )
                    {
                        if ( ResourceNotFoundError.class.isAssignableFrom( error.getClass() ) )
                        {
                            return new ErrorResolution( HttpServletResponse.SC_NOT_FOUND, error.getMessage( null ) );
                        }
                    }
                }

                return resolution;
            } 
            else if ( ctx.getLifecycleStage() == LifecycleStage.CustomValidation )
            {
                // Nothing to process before validation occurs
                Resolution resolution = ctx.proceed();

                // Now check for validation errors.  If any exist, then return the error resolution
                ValidationErrors validationErrors = ctx.getActionBeanContext().getValidationErrors();

                Log.getInstance( getClass() ).debug( "(", ctx.getActionBean().getClass(), ") Checking for validation errors : ", ctx.getLifecycleStage().name() );

                if ( validationErrors != null && !validationErrors.isEmpty() )
                {
                    Log.getInstance( getClass() ).debug( "(", ctx.getActionBean().getClass(), ") Found validation errors : ", ctx.getLifecycleStage().name() );

                    Map< Object, Object> jsonErrorMap = new HashMap< Object, Object>();

                    // First, append the global errors -- if any
                    List< String> jsonGlobalErrors = new ArrayList< String>();

                    if ( !validationErrors.hasFieldErrors() )
                    {
                        Log.getInstance( getClass() ).debug( "(", ctx.getActionBean().getClass(), ") Found global errors : ", ctx.getLifecycleStage().name() );

                        List< ValidationError> globalErrors = validationErrors.get( ValidationErrors.GLOBAL_ERROR );

                        for ( ValidationError validationError : globalErrors )
                        {
                            jsonGlobalErrors.add( validationError.getMessage( null ) );
                        }

                    }

                    jsonErrorMap.put( "globalErrors", jsonGlobalErrors );

                    ArrayList< Map< String, Object>> allFieldErrors = new ArrayList< Map< String, Object>>();

                    // Next, append the field errors -- if any
                    if ( validationErrors.hasFieldErrors() )
                    {
                        Log.getInstance( getClass() ).debug( "(", ctx.getActionBean().getClass(), ") Found field errors : ", ctx.getLifecycleStage().name() );

                        for ( String fieldName : validationErrors.keySet() )
                        {
                            if ( !fieldName.equals( ValidationErrors.GLOBAL_ERROR ) )
                            {
                                List< ValidationError> fieldValidationErrors = validationErrors.get( fieldName );
                                Map< String, Object> fieldErrors = new HashMap< String, Object>();
                                fieldErrors.put( "fieldName", fieldName );
                                fieldErrors.put( "fieldValue", fieldValidationErrors.get( 0 ).getFieldValue() );

                                List< String> fieldErrorMessages = new ArrayList< String>();
                                for ( ValidationError validationError : fieldValidationErrors )
                                {
                                    fieldErrorMessages.add( validationError.getMessage( null ) );
                                }

                                fieldErrors.put( "errorMessages", fieldErrorMessages );

                                allFieldErrors.add( fieldErrors );
                            }
                        }

                        jsonErrorMap.put( "fieldErrors", allFieldErrors );
                    }

                    final JsonBuilder jsonBuilder = new JsonBuilder( jsonErrorMap );
                    final byte[] errorBytes = jsonBuilder.build().getBytes();

                    Log.getInstance( getClass() ).debug( "(", ctx.getActionBean().getClass(), ") Returning validation error resolution : ", ctx.getLifecycleStage().name(), " | Contents => " + errorBytes );

                    return new StreamingResolution( "application/json" )
                    {
                        @Override
                        protected void stream( HttpServletResponse response ) throws Exception
                        {
                            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
                            response.getOutputStream().write( errorBytes );
                            response.flushBuffer();
                        }
                    };
                }

                return resolution;
            }
            else if ( ctx.getLifecycleStage() == LifecycleStage.EventHandling )
            // After execute the event handler and catch any unhandled exceptions
            // that may occur so they can be converted to a proper JSON error
            // response.
            {
                try
                {
                    // Execute the event
                    return ctx.proceed();
                }
                catch ( Throwable e )
                {
                    Log.getInstance( getClass() ).error( e, "(", ctx.getActionBean().getClass(), ") Unhandled exception occurred executing handler." );

                    // If an error occurs, we need to create a well-formed
                    // JSON error response and return it back to the caller.
                    Map< Object, Object> jsonErrorMap = new HashMap< Object, Object>();

                    // First, append the global errors -- if any
                    List< String> jsonGlobalErrors = new ArrayList< String>();

                    jsonGlobalErrors.add( "Unexpected error occurred executing this API call: " + e.getCause().getMessage() );
                    jsonErrorMap.put( "globalErrors", jsonGlobalErrors );
                    final JsonBuilder jsonBuilder = new JsonBuilder( jsonErrorMap );
                    final byte[] errorBytes = jsonBuilder.build().getBytes();

                    Log.getInstance( getClass() ).debug( "(", ctx.getActionBean().getClass(), ") Returning validation error resolution : ", ctx.getLifecycleStage().name(), " | Content => " + errorBytes );

                    return new StreamingResolution( "application/json" )
                    {
                        @Override
                        protected void stream( HttpServletResponse response ) throws Exception
                        {
                            response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                            response.getOutputStream().write( errorBytes );
                            response.flushBuffer();
                        }
                    };
                }
            }
            else
            {
                return ctx.proceed();
            }
        }
        else
        {
            return ctx.proceed();
        }
    }

}
