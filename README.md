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

## License

This distribution is licensed under the terms of the Apache License, Version 2.0 (see LICENSE.txt).
