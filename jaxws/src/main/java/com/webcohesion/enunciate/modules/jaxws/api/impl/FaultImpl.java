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
package com.webcohesion.enunciate.modules.jaxws.api.impl;

import com.webcohesion.enunciate.api.Styles;
import com.webcohesion.enunciate.api.datatype.BaseType;
import com.webcohesion.enunciate.api.datatype.DataType;
import com.webcohesion.enunciate.api.datatype.DataTypeReference;
import com.webcohesion.enunciate.api.datatype.Example;
import com.webcohesion.enunciate.api.services.Fault;
import com.webcohesion.enunciate.facets.Facet;
import com.webcohesion.enunciate.javac.javadoc.JavaDoc;
import com.webcohesion.enunciate.modules.jaxws.model.WebFault;
import com.webcohesion.enunciate.util.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ryan Heaton
 */
public class FaultImpl implements Fault, DataTypeReference {

  private final WebFault fault;

  public FaultImpl(WebFault fault) {
    this.fault = fault;
  }

  @Override
  public String getName() {
    return this.fault.getMessageName();
  }

  @Override
  public String getConditions() {
    return this.fault.getConditions();
  }

  @Override
  public DataTypeReference getDataType() {
    return this;
  }

  @Override
  public Example getExample() {
    return null;
  }

  @Override
  public BaseType getBaseType() {
    return BaseType.object;
  }

  @Override
  public String getLabel() {
    String value = getName();

    String specifiedLabel = AnnotationUtils.getSpecifiedLabel(this.fault);
    if (specifiedLabel != null) {
      value = specifiedLabel;
    }

    return value;
  }

  @Override
  public String getSlug() {
    //todo: faults as data types
    return null;
  }

  @Override
  public List<ContainerType> getContainers() {
    return Collections.emptyList();
  }

  @Override
  public DataType getValue() {
    //todo: faults as data types
    return null;
  }

  @Override
  public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
    return this.fault.getAnnotation(annotationType);
  }

  @Override
  public Map<String, AnnotationMirror> getAnnotations() {
    return this.fault.getAnnotations();
  }

  @Override
  public Set<Facet> getFacets() {
    return this.fault.getFacets();
  }

  @Override
  public JavaDoc getJavaDoc() {
    return this.fault.getJavaDoc();
  }

  @Override
  public Set<String> getStyles() {
    return Styles.gatherStyles(this.fault, this.fault.getContext().getContext().getConfiguration().getAnnotationStyles());
  }

  @Override
  public String getBaseTypeFormat() {
    return null;
  }
}
