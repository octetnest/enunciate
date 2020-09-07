/**
 * Copyright © 2006-2016 Web Cohesion (info@webcohesion.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webcohesion.enunciate.modules.swagger;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.webcohesion.enunciate.api.datatype.*;
import com.webcohesion.enunciate.javac.javadoc.JavaDoc;
import com.webcohesion.enunciate.metadata.DocumentationExample;
import com.webcohesion.enunciate.util.freemarker.FreemarkerUtil;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.util.List;

/**
 * Template method used to determine the objective-c "simple name" of an accessor.
 *
 * @author Ryan Heaton
 */
public class JsonExampleForMethod implements TemplateMethodModelEx {

  private JsonStringEncoder encoder = new JsonStringEncoder();

  public Object exec(List list) throws TemplateModelException {
    if (list.size() < 1) {
      throw new TemplateModelException("The jsonExampleFor method must have a parameter.");
    }

    TemplateModel from = (TemplateModel) list.get(0);
    Object unwrapped = FreemarkerUtil.unwrap(from);
    DataType dataType = null;
    String specifiedExample = null;
    BaseType baseType = null;
    if (unwrapped instanceof Property) {
      Property property = (Property) unwrapped;
      baseType = property.getDataType() != null ? property.getDataType().getBaseType() : null;
      specifiedExample = findSpecifiedExample(property);
    }
    else if (unwrapped instanceof DataTypeReference) {
      dataType = ((DataTypeReference) unwrapped).getValue();
      baseType = ((DataTypeReference) unwrapped).getBaseType();
    }
    else if (unwrapped instanceof DataType) {
      dataType = (DataType) unwrapped;
      baseType = ((DataType) unwrapped).getBaseType();
    }

    if (baseType != null) {
      switch (baseType) {
        case object:
          if (dataType != null) {
            if (dataType.getBaseType() == BaseType.object) {
              Example example = dataType.getExample();
              if (example != null) {
                return example.getBody();
              }
            }
          }
        default:
          return specifiedExample;
      }
    }

    return null;
  }

  private String findSpecifiedExample(Property property) {
    String example = null;

    JavaDoc.JavaDocTagList tags = property.getJavaDoc().get("documentationExample");
    if (tags != null && tags.size() > 0) {
      String tag = tags.get(0).trim();
      example = tag.isEmpty() ? null : tag;
    }

    DocumentationExample documentationExample = property.getAnnotation(DocumentationExample.class);
    if (documentationExample != null) {
      if (documentationExample.exclude()) {
        return null;
      }

      example = documentationExample.value();
      example = "##default".equals(example) ? null : example;
    }

    if (example != null && (property.getDataType() == null || property.getDataType().getBaseType() == BaseType.string)) {
      example = "\"" + new String(encoder.quoteAsString(example)) + "\"";
    }

    return example;
  }
}