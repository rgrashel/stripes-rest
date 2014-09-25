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

import java.util.HashMap;
import java.util.Map;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;

@UrlBinding( "/stripes-rest" )
public class ExampleRestActionBean implements RestApiActionBean
{
    @Validate( on = "head", required = true )
    private String id;
    
    public Resolution get()
    {
        Map< String, String > response = new HashMap< String, String >();
        response.put( "foo", "bar" );
        response.put( "hello", "world" );
        
        return new JsonStreamingResolution( response );
    }
    
    @ValidationMethod( on = "head" )
    public void validateHeadCall( ValidationErrors errors )
    {
        errors.addGlobalError( new SimpleError( "The head request was not valid for whatever custom reason." ) );
    }
    
    public Resolution head()
    {
        return new JsonStreamingResolution( "Successful head!" );
    }
    
    public void setId( String id )
    {
        this.id = id;
    }
    
    public String getId()
    {
        return this.id;
    }
    
    private ActionBeanContext context;
    
    public ActionBeanContext getContext()
    {
        return this.context;
    }
    
    public void setContext( ActionBeanContext context )
    {
        this.context = context;
    }
    
}
