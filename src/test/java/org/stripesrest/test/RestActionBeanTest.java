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
package org.stripesrest.test;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;
import net.sourceforge.stripes.util.Log;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.stripesrest.JsonResolution;
import org.stripesrest.POST;
import org.stripesrest.RestActionBean;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This is a series of tests for Stripes REST action beans.
 */
public class RestActionBeanTest implements ActionBean, RestActionBean
{

    private static final Log log = Log.getInstance( RestActionBeanTest.class );

    @Validate( on = "head", required = true )
    private String id;

    private MockServletContext context;

    @BeforeClass
    public void initCtx()
    {
        context = StripesTestFixture.createServletContext();
    }

    @AfterClass
    public void closeCtx()
    {
        context.close();
    }

    public MockServletContext getMockServletContext()
    {
        return context;
    }

    @DefaultHandler
    public Resolution get()
    {
        Map< String, Object> response = new HashMap< String, Object>();
        response.put( "foo", "bar" );
        response.put( "hello", "world" );

        Map< String, Number> nested = new HashMap< String, Number>();
        nested.put( "one", 1 );
        nested.put( "two", 2 );

        response.put( "numbers", nested );

        return new JsonResolution( response );
    }

    @POST
    public Resolution runtimeErrorPost()
    {
        throw new RuntimeException( "This is a completely unhandled exception." );
    }

    @ValidationMethod( on = "head" )
    public void validateHeadCall( ValidationErrors errors )
    {
        errors.addGlobalError( new SimpleError( "The head request was not valid for whatever custom reason." ) );
    }

    public Resolution head()
    {
        return new JsonResolution( "Successful head!" );
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getId()
    {
        return this.id;
    }

    private ActionBeanContext actionBeanContext;

    public ActionBeanContext getContext()
    {
        return this.actionBeanContext;
    }

    public void setContext( ActionBeanContext actionBeanContext )
    {
        this.actionBeanContext = actionBeanContext;
    }

    @Test
    public void successfulGet() throws Exception
    {
        MockRoundtrip trip = new MockRoundtrip( getMockServletContext(), getClass() );
        trip.getRequest().setMethod( "GET" );
        trip.execute();

        Assert.assertEquals( trip.getResponse().getStatus(), HttpServletResponse.SC_OK );
        logTripResponse( trip );
    }

    @Test
    public void failedPost() throws Exception
    {
        MockRoundtrip trip = new MockRoundtrip( getMockServletContext(), getClass() );
        trip.getRequest().setMethod( "POST" );
        trip.execute();
        Assert.assertNotEquals( trip.getResponse().getStatus(), HttpServletResponse.SC_OK );
        logTripResponse( trip );
    }

    @Test
    public void missingRequiredParameterOnHead() throws Exception
    {
        MockRoundtrip trip = new MockRoundtrip( getMockServletContext(), getClass() );
        trip.getRequest().setMethod( "HEAD" );
        trip.execute();
        Assert.assertNotEquals( trip.getResponse().getStatus(), HttpServletResponse.SC_OK );
        logTripResponse( trip );
    }

    @Test
    public void failedCustomValidationOnHead() throws Exception
    {
        MockRoundtrip trip = new MockRoundtrip( getMockServletContext(), getClass() );
        trip.setParameter( "id", "SOME_ID" );
        trip.getRequest().setMethod( "HEAD" );
        trip.execute();
        Assert.assertNotEquals( trip.getResponse().getStatus(), HttpServletResponse.SC_OK );
        Assert.assertTrue( trip.getValidationErrors() != null && !trip.getValidationErrors().isEmpty() );
        logTripResponse( trip );
    }

    @Test
    public void testUnhandledExceptionAtRuntime() throws Exception
    {
        MockRoundtrip trip = new MockRoundtrip( getMockServletContext(), getClass() );
        trip.getRequest().setMethod( "POST" );
        trip.execute( "runtimeErrorPost" );
        Assert.assertEquals( trip.getResponse().getStatus(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
        logTripResponse( trip );
    }

    private void logTripResponse( MockRoundtrip trip )
    {
        log.debug( "TRIP RESPONSE: [Event=" + trip.getActionBean( getClass() ).getContext().getEventName() + "] [Status=" + trip.getResponse().getStatus()
                + "] [Message=" + trip.getResponse().getOutputString() + "] [Error Message="
                + trip.getResponse().getErrorMessage() + "]" );
    }
}
