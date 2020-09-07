/**
 * Copyright © 2006-2016 Web Cohesion (info@webcohesion.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webcohesion.enunciate.modules.jackson.model;

import com.webcohesion.enunciate.EnunciateException;
import com.webcohesion.enunciate.metadata.qname.XmlQNameEnum;
import com.webcohesion.enunciate.metadata.qname.XmlQNameEnumValue;
import com.webcohesion.enunciate.metadata.qname.XmlUnknownQNameEnumValue;
import com.webcohesion.enunciate.modules.jackson.EnunciateJacksonContext;
import com.webcohesion.enunciate.modules.jackson.model.types.KnownJsonType;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.xml.bind.annotation.XmlSchema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.webcohesion.enunciate.util.AnnotationUtils.isIgnored;

/**
 * A qname enum type definition.
 *
 * @author Ryan Heaton
 */
public class QNameEnumTypeDefinition extends EnumTypeDefinition {

  private final String namespace;

  public QNameEnumTypeDefinition(TypeElement delegate, EnunciateJacksonContext context) {
    super(delegate, context);

    XmlQNameEnum xmlQNameEnum = getAnnotation(XmlQNameEnum.class);
    if (xmlQNameEnum == null) {
      throw new IllegalArgumentException(delegate.getQualifiedName() + " is not a qname enum (not annotated with @com.webcohesion.enunciate.metadata.qname.XmlQNameEnum)");
    }
    else if (xmlQNameEnum.base() != XmlQNameEnum.BaseType.URI) {
      throw new EnunciateException(String.format("Qname enum %s cannot be serialized to JSON because its base is %s.", delegate, xmlQNameEnum.base()));
    }

    XmlSchema schemaInfo = getPackage().getAnnotation(XmlSchema.class);
    String namespace = schemaInfo == null ? "" : schemaInfo.namespace();
    if (!"##default".equals(xmlQNameEnum.namespace())) {
      namespace = xmlQNameEnum.namespace();
    }
    this.namespace = namespace;
  }

  @Override
  protected KnownJsonType loadBaseType(TypeElement delegate) {
    return KnownJsonType.STRING;
  }

  @Override
  protected List<EnumValue> loadEnumValues() {
    List<VariableElement> enumConstants = enumValues();
    List<EnumValue> enumValueMap = new ArrayList<EnumValue>();
    HashSet<String> enumValues = new HashSet<String>(enumConstants.size());
    VariableElement unknownQNameConstant = null;
    for (VariableElement enumConstant : enumConstants) {
      if (isIgnored(enumConstant)) {
        continue;
      }

      XmlUnknownQNameEnumValue unknownQNameEnumValue = enumConstant.getAnnotation(XmlUnknownQNameEnumValue.class);
      if (unknownQNameEnumValue != null) {
        if (unknownQNameConstant != null) {
          throw new EnunciateException(getQualifiedName() + ": no more than two constants can be annotated with @XmlUnknownQNameEnumValue.");
        }

        unknownQNameConstant = enumConstant;
        continue;
      }

      String ns = this.namespace;
      String localPart = enumConstant.getSimpleName().toString();
      XmlQNameEnumValue enumValueInfo = enumConstant.getAnnotation(XmlQNameEnumValue.class);
      if (enumValueInfo != null) {
        if (enumValueInfo.exclude()) {
          continue;
        }

        if (!"##default".equals(enumValueInfo.namespace())) {
          ns = enumValueInfo.namespace();
        }
        if (!"##default".equals(enumValueInfo.localPart())) {
          localPart = enumValueInfo.localPart();
        }
      }

      String uri = ns + localPart;
      if (!enumValues.add(uri)) {
        throw new EnunciateException(getQualifiedName() + ": duplicate qname enum value: " + uri);
      }

      enumValueMap.add(new EnumValue(this, enumConstant, enumConstant.getSimpleName().toString(), uri));
    }

    if (unknownQNameConstant != null) {
      //enter the unknown qname constant as a null qname.
      enumValueMap.add(new EnumValue(this, unknownQNameConstant, unknownQNameConstant.getSimpleName().toString(), null));
    }
    
    return enumValueMap;
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return true;
  }

  public boolean isQNameEnum() {
    return true;
  }

}
