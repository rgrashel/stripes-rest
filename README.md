# Stripes REST ActionBean Framework

The Stripes REST ActionBean Framework provides a simple and non-intrustive way to create truly RESTful services within your application 
using all of the same capabilities within Stripes itself.  With this framework, you can easily create action beans which allow for 
all HTTP verbs and have responses (Resolutions) automatically serialized into JSON format.  

* There are no external dependencies for this framework.  It depends on a small subset of the libraries that Stripes itself depends on.
* JSON serialization is performed by a combination of the Stripes native JavascriptBuilder and the JDK's scripting engine 

## Configuration

### Maven Configuration

Add the Stripes REST ActionBean Framework dependency to your project:

```xml
<dependency>
    <groupId>org.stripesrest</groupId>
    <artifactId>stripesrest</artifactId>
    <version>VERSION</version>
</dependency>
```

### Stripes filter configuration

Add Stripes Rest Interceptor to the Stripes filter `Extension.Packages` configuration in `web.xml`:

```xml
<init-param>
    <param-name>Extension.Packages</param-name>
    <param-value>org.stripesrest</param-value>
</init-param>
```

## Creating a RESTful Action Bean

Creating a RESTful Action Bean requires only the following two things:

1) Have your action bean implement the (empty) RestActionBean interface
2) Provide an event handler method that is the same name as the HTTP verb you wish to implement: get(), post(), put(), delete(), head().

The Stripes REST Action Bean Interceptor will detect when your REST service is called and automatically invoke the proper HTTP verb event handler method.  

**NOTE:** POST verbs accept parameters using standard (application/x-www-form-urlencoded) HTTP format.

## Examples

### Examples REST ActionBean To Add Two Numbers Using A POST verb, create a result, and return the result as JSON.

```java
@UrlBinding( "/sum" )
public CalculatorRestActionBean implements RestActionBean
{
    @Validate( on = "post", required = true)
    private int firstNumber;
    
    @Validate( on = "post", required = true)
    private int secondNumber;
    
    /**
     * POST event handler.  Takes the two numbers, adds them together, and returns the result.
     */
    public Resolution post()
    {
        Map< String, Number > response = new HashMap<>();
        response.put( "firstNumber", firstNumber );
        response.put( "secondNumber", secondNumber );
        response.put( "sum", firstNumber + secondNumber );
        
        return new JsonResolution( response );
    }
    
    public void setActionBeanContext( ActionBeanContext actionBeanContext ) { this.actionBeanContext = actionBeanContext; }
    public ActionBeanContext getActionBeanContext() { return this.actionBeanContext; }
    private ActionBeanContext actionBeanContext();
}

```

Accessing this service via a CURL call would look like this:

```text
curl -i -X POST -d "firstNumber=2" -d "secondNumber=2" http://<hostname>/sum  
```

It's that easy!  Stripes will still handle all of the validation, type conversion, and @Before/@After just like it
normally would.  The JsonResolution will take any Java object, navigate it reflectively, and serialize it to JSON.  Internally, the JsonResolution uses the same JavascriptBuilder that JavascriptResolution does.

## Validation Errors

Any validation errors that occur while processing a HTTP verb will result in an ErrorResolution being returned to the caller.  The ErrorResolution will contain a JSON structure that has a collection of both global errors and field errors along with a
HTTP "Bad Request" error code (400).

## HTTP Verb Calls Made To REST Action Beans Which Are Not Implemented

If a caller tries to access a REST action bean that does not implement the HTTP verb/method, then an ErrorResolution will be 
returned back to the caller with a "Method Not Allowed" HTTP error code (405).

## License

This distribution is licensed under the terms of the Apache License, Version 2.0 (see LICENSE.txt).
