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
package com.webcohesion.enunciate.modules.jaxb.util;

import com.webcohesion.enunciate.modules.jaxb.model.Accessor;
import com.webcohesion.enunciate.modules.jaxb.model.TypeDefinition;
import com.webcohesion.enunciate.modules.jaxb.model.types.XmlClassType;
import com.webcohesion.enunciate.modules.jaxb.model.types.XmlType;
import com.webcohesion.enunciate.util.freemarker.FreemarkerUtil;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ryan Heaton
 */
public class AccessorOverridesAnotherMethod implements TemplateMethodModelEx {

  public Object exec(List list) throws TemplateModelException {
    if (list.size() < 1) {
      throw new TemplateModelException("The accessorOverridesAnother method must have the accessor as a parameter.");
    }

    TemplateModel from = (TemplateModel) list.get(0);
    Object unwrapped = FreemarkerUtil.unwrap(from);
    if (!(unwrapped instanceof Accessor)) {
      throw new TemplateModelException("The accessorOverridesAnother method must have the accessor as a parameter.");
    }

    return overridesAnother((Accessor) unwrapped);
  }

  public Boolean overridesAnother(Accessor a) {
    TypeDefinition typeDefinition = a.getTypeDefinition();
    XmlType baseType = typeDefinition.getBaseType();
    if (baseType instanceof XmlClassType) {
      typeDefinition = ((XmlClassType) baseType).getTypeDefinition();

      while (typeDefinition != null) {
        ArrayList<Accessor> accessors = new ArrayList<Accessor>();
        accessors.addAll(typeDefinition.getAttributes());
        accessors.add(typeDefinition.getValue());
        accessors.addAll(typeDefinition.getElements());
        for (Accessor accessor : accessors) {
          if (a.overrides(accessor)) {
            return true;
          }
        }

        baseType = typeDefinition.getBaseType();
        typeDefinition = baseType instanceof XmlClassType ? ((XmlClassType) baseType).getTypeDefinition() : null;
      }
    }

    return Boolean.FALSE;
  }

}
