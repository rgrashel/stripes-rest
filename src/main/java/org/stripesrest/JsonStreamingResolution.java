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

import com.google.gson.GsonBuilder;
import net.sourceforge.stripes.action.StreamingResolution;

/**
 * This resolution is intended to be used with Stripes REST action beans.  This
 * type of resolution extends streaming resolution and will take a Java object 
 * and serialize it to JSON automatically.
 */
public class JsonStreamingResolution extends StreamingResolution
{
    /**
     * This constructor should be used if the caller has already serialized
     * the object into JSON.
     * 
     * @param rawJsonText - Raw text JSON string
     */
    public JsonStreamingResolution( String rawJsonText )
    {
        super( "application/json", rawJsonText );
    }
    
    /**
     * This constructor should be used if the caller wants to return an
     * object and have it automatically serialized into JSON.
     * 
     * @param objectToSerialize - Object to serialize into JSON 
     */
    public JsonStreamingResolution( Object objectToSerialize )
    {
        super( "application/json", new GsonBuilder().setPrettyPrinting().create().toJson( objectToSerialize ) );
    }
}
