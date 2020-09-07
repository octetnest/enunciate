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

import com.webcohesion.enunciate.api.resources.Entity;
import com.webcohesion.enunciate.api.resources.MediaTypeDescriptor;
import com.webcohesion.enunciate.util.freemarker.FreemarkerUtil;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Template method used to determine the objective-c "simple name" of an accessor.
 *
 * @author Ryan Heaton
 */
public class UniqueMediaTypesForMethod implements TemplateMethodModelEx {

  public Object exec(List list) throws TemplateModelException {
    if (list.size() < 1) {
      throw new TemplateModelException("The uniqueMediaTypesFor method must have a parameter.");
    }

    TemplateModel from = (TemplateModel) list.get(0);
    Object unwrapped = FreemarkerUtil.unwrap(from);
    HashMap<String, MediaTypeDescriptor> uniqueMediaTypes = new HashMap<String, MediaTypeDescriptor>();
    if (unwrapped instanceof Entity) {
      Entity entity = (Entity) unwrapped;
      List<? extends MediaTypeDescriptor> mts = entity.getMediaTypes();
      if (mts != null) {
        for (MediaTypeDescriptor mt : mts) {
          uniqueMediaTypes.put(mt.getMediaType(), mt);
        }
      }
    }
    ArrayList<MediaTypeDescriptor> orderedTypes = new ArrayList<>(uniqueMediaTypes.values());
    Collections.sort(orderedTypes, (m1, m2) -> {
      String syntax1 = m1.getSyntax() == null ? "" : m1.getSyntax();
      String syntax2 = m2.getSyntax() == null ? "" : m2.getSyntax();
      return syntax1.compareTo(syntax2);
    });
    return orderedTypes;
  }
}
