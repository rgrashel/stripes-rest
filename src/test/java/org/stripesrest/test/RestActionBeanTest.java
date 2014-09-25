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
import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;
import org.stripesrest.ExampleRestActionBean;
import org.testng.annotations.Test;

/**
 * This is a series of tests for Stripes REST action beans.
 */
public class RestActionBeanTest
{
    private MockServletContext getMockServletContext()
    {
        MockServletContext ctx = new MockServletContext("test");

        // Add the Stripes Filter
        Map<String, String> filterParams = new HashMap<String, String>();
        filterParams.put("ActionResolver.Packages", "org.stripesrest");
        filterParams.put("Interceptor.Classes", "org.stripesrest.RestActionInterceptor");
        ctx.addFilter(StripesFilter.class, "StripesFilter", filterParams);

        // Add the Stripes Dispatcher
        ctx.setServlet(DispatcherServlet.class, "StripesDispatcher", null);
        
        return ctx;
    }

    @Test
    public void successfulGet() throws Exception
    {
        MockRoundtrip trip = new MockRoundtrip(getMockServletContext(), ExampleRestActionBean.class);
        trip.getRequest().setMethod("get");
        trip.execute();

        System.out.println(trip.getOutputString());
    }

    @Test
    public void failedPost() throws Exception
    {
        MockRoundtrip trip = new MockRoundtrip( getMockServletContext(), ExampleRestActionBean.class );
        trip.getRequest().setMethod("post");
        trip.execute();
        System.out.println(trip.getResponse().getStatus() + " | Error Message : " + trip.getResponse().getErrorMessage() );
    }
    
    @Test
    public void missingRequiredParameterOnHead() throws Exception
    {
        MockRoundtrip trip = new MockRoundtrip( getMockServletContext(), ExampleRestActionBean.class );
        trip.getRequest().setMethod("head");
        trip.execute();
        System.out.println(trip.getResponse().getStatus() + " | Error Message : " + trip.getResponse().getErrorMessage() );
    }
    
    @Test
    public void failedCustomValidationOnHead() throws Exception
    {
        MockRoundtrip trip = new MockRoundtrip( getMockServletContext(), ExampleRestActionBean.class );
        trip.setParameter("id", "SOME_ID");
        trip.getRequest().setMethod("head");
        trip.execute();
        System.out.println(trip.getResponse().getStatus() + " | Error Message : " + trip.getResponse().getErrorMessage() );
    }    
}
