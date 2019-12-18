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
package com.webcohesion.enunciate.modules.jaxb.model.util;

import com.webcohesion.enunciate.EnunciateException;
import com.webcohesion.enunciate.javac.decorations.Annotations;
import com.webcohesion.enunciate.javac.decorations.DecoratedProcessingEnvironment;
import com.webcohesion.enunciate.javac.decorations.type.DecoratedDeclaredType;
import com.webcohesion.enunciate.javac.decorations.type.DecoratedTypeMirror;
import com.webcohesion.enunciate.javac.decorations.type.DecoratedWildcardType;
import com.webcohesion.enunciate.javac.decorations.type.TypeMirrorUtils;
import com.webcohesion.enunciate.modules.jaxb.EnunciateJaxbContext;
import com.webcohesion.enunciate.modules.jaxb.model.Accessor;
import com.webcohesion.enunciate.modules.jaxb.model.adapters.AdapterType;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Consolidation of common logic for implementing the JAXB contract.
 * 
 * @author Ryan Heaton
 */
@SuppressWarnings ( "unchecked" )
public class JAXBUtil {

  private static final String ADAPTERS_BY_PACKAGE_PROPERTY = "com.webcohesion.enunciate.modules.jaxb.model.util.JAXBUtil#ADAPTERS_BY_PACKAGE";

  protected JAXBUtil() {}

  public static DecoratedDeclaredType getNormalizedCollection(DecoratedTypeMirror typeMirror, DecoratedProcessingEnvironment env) {
    DecoratedDeclaredType base = typeMirror.isList() ? TypeMirrorUtils.listType(env) : typeMirror.isCollection() ? TypeMirrorUtils.collectionType(env) : null;

    if (base != null) {
      //now narrow the component type to what can be valid xml.
      DecoratedTypeMirror componentType = findCollectionComponentType((DeclaredType) typeMirror, env);
      base = (DecoratedDeclaredType) env.getTypeUtils().getDeclaredType((TypeElement) base.asElement(), componentType);
    }

    return base;
  }

  private static DecoratedTypeMirror findCollectionComponentType(DeclaredType typeMirror, DecoratedProcessingEnvironment env) {
    DecoratedTypeMirror componentType = TypeMirrorUtils.objectType(env);
    typeMirror = findCollectionDeclaration(typeMirror, env);
    if (typeMirror != null) {
      List<? extends DecoratedTypeMirror> typeArgs = (List<? extends DecoratedTypeMirror>) typeMirror.getTypeArguments();
      if (typeArgs.size() == 1) {
        componentType = typeArgs.get(0);
        if (componentType.isWildcard()) {
          componentType = (DecoratedTypeMirror) ((DecoratedWildcardType) componentType).getExtendsBound();
        }
        Element element = componentType == null ? null : env.getTypeUtils().asElement(componentType);

        //the interface isn't adapted, check for @XmlTransient and if it's there, narrow it to java.lang.Object.
        //see https://jira.codehaus.org/browse/ENUNCIATE-660
        if (element == null || (element.getAnnotation(XmlJavaTypeAdapter.class) == null && element.getAnnotation(XmlTransient.class) != null)) {
          componentType = TypeMirrorUtils.objectType(env);
        }

      }
    }

    return componentType;
  }

  private static DeclaredType findCollectionDeclaration(DeclaredType typeMirror, ProcessingEnvironment env) {
    TypeElement element = (TypeElement) typeMirror.asElement();

    if (element != null) {
      if (Collection.class.getName().equals(element.getQualifiedName().toString())) {
        return typeMirror;
      }
      else {
        List<? extends TypeMirror> supertypes = env.getTypeUtils().directSupertypes(typeMirror);
        for (TypeMirror supertype : supertypes) {
          if (supertype instanceof DeclaredType) {
            DeclaredType collectionDeclaration = findCollectionDeclaration((DeclaredType) supertype, env);
            if (collectionDeclaration != null) {
              return collectionDeclaration;
            }
          }
        }
      }
    }

    return null;
  }

  /**
   * Finds the adapter type for the specified declaration, if any.
   *
   * @param declaration The declaration for which to find that adapter type.
   * @param context The context.
   * @return The adapter type, or null if none was specified.
   */
  public static AdapterType findAdapterType(Element declaration, EnunciateJaxbContext context) {
    DecoratedProcessingEnvironment env = context.getContext().getProcessingEnvironment();
    if (declaration instanceof Accessor) {
      //jaxb accessor can be adapted.
      Accessor accessor = ((Accessor) declaration);
      return findAdapterType(accessor.getAccessorType(), accessor, env.getElementUtils().getPackageOf(accessor.getTypeDefinition()), context);
    }
    else if (declaration instanceof ExecutableElement) {
      //assume the return type of the method is adaptable (e.g. web results, fault bean getters).
      ExecutableElement method = ((ExecutableElement) declaration);
      return findAdapterType((DecoratedTypeMirror) method.getReturnType(), method, env.getElementUtils().getPackageOf(method), context);
    }
    else if (declaration instanceof TypeElement) {
      return findAdapterType((DecoratedDeclaredType) declaration.asType(), null, null, context);
    }
    else {
      throw new IllegalArgumentException("A " + declaration.getClass().getSimpleName() + " is not an adaptable declaration according to the JAXB spec.");
    }
  }

  protected static AdapterType findAdapterType(DecoratedTypeMirror maybeContainedAdaptedType, Element referer, PackageElement pckg, EnunciateJaxbContext context) {
    DecoratedProcessingEnvironment env = context.getContext().getProcessingEnvironment();
    TypeMirror adaptedType = TypeMirrorUtils.getComponentType(maybeContainedAdaptedType, env);
    adaptedType = adaptedType == null ? maybeContainedAdaptedType : adaptedType;
    XmlJavaTypeAdapter typeAdapterInfo = referer != null ? referer.getAnnotation(XmlJavaTypeAdapter.class) : null;
    if (adaptedType instanceof DeclaredType) {
      if (typeAdapterInfo == null) {
        typeAdapterInfo = ((DeclaredType) adaptedType).asElement().getAnnotation(XmlJavaTypeAdapter.class);
      }

      if ((typeAdapterInfo == null) && (pckg != null)) {
        TypeElement typeDeclaration = (TypeElement) ((DeclaredType) adaptedType).asElement();
        typeAdapterInfo = getAdaptersOfPackage(pckg, context).get(typeDeclaration.getQualifiedName().toString());
      }
    }

    if (typeAdapterInfo != null) {
      final XmlJavaTypeAdapter finalInfo = typeAdapterInfo;
      DecoratedTypeMirror adapterTypeMirror = Annotations.mirrorOf(new Callable<Class<?>>() {
        @Override
        public Class<?> call() throws Exception {
          return finalInfo.value();
        }
      }, env);
      if (adapterTypeMirror instanceof DecoratedDeclaredType) {
        AdapterType adapterType = new AdapterType((DecoratedDeclaredType) adapterTypeMirror, context.getContext());
        if (!context.getContext().getProcessingEnvironment().getTypeUtils().isSameType(adapterType.getAdaptingType(), adaptedType)) {
          return adapterType;
        }
      }
    }

    return null;

  }

  /**
   * Gets the adapters of the specified package.
   *
   * @param pckg the package for which to get the adapters.
   * @param context The context.
   * @return The adapters for the package.
   */
  private static Map<String, XmlJavaTypeAdapter> getAdaptersOfPackage(PackageElement pckg, EnunciateJaxbContext context) {
    if (pckg == null) {
      return null;
    }

    Map<String, Map<String, XmlJavaTypeAdapter>> adaptersOfAllPackages = (Map<String, Map<String, XmlJavaTypeAdapter>>) context.getContext().getProperty(ADAPTERS_BY_PACKAGE_PROPERTY);
    if (adaptersOfAllPackages == null) {
      adaptersOfAllPackages = new HashMap<String, Map<String, XmlJavaTypeAdapter>>();
      context.getContext().setProperty(ADAPTERS_BY_PACKAGE_PROPERTY, adaptersOfAllPackages);
    }
    Map<String, XmlJavaTypeAdapter> adaptersOfPackage = adaptersOfAllPackages.get(pckg.getQualifiedName().toString());

    if (adaptersOfPackage == null) {
      adaptersOfPackage = new HashMap<String, XmlJavaTypeAdapter>();
      adaptersOfAllPackages.put(pckg.getQualifiedName().toString(), adaptersOfPackage);

      XmlJavaTypeAdapter javaType = pckg.getAnnotation(XmlJavaTypeAdapter.class);
      XmlJavaTypeAdapters javaTypes = pckg.getAnnotation(XmlJavaTypeAdapters.class);

      if ((javaType != null) || (javaTypes != null)) {
        ArrayList<XmlJavaTypeAdapter> allAdaptedTypes = new ArrayList<XmlJavaTypeAdapter>();
        if (javaType != null) {
          allAdaptedTypes.add(javaType);
        }

        if (javaTypes != null) {
          allAdaptedTypes.addAll(Arrays.asList(javaTypes.value()));
        }

        for (final XmlJavaTypeAdapter adaptedTypeInfo : allAdaptedTypes) {
          DecoratedTypeMirror typeMirror = Annotations.mirrorOf(new Callable<Class<?>>() {
            @Override
            public Class<?> call() throws Exception {
              return adaptedTypeInfo.type();
            }
          }, context.getContext().getProcessingEnvironment(), XmlJavaTypeAdapter.DEFAULT.class);

          if (typeMirror == null) {
            throw new EnunciateException("Package " + pckg.getQualifiedName() + ": a type must be specified in " + XmlJavaTypeAdapter.class.getName() + " at the package-level.");
          }

          if (!(typeMirror instanceof DeclaredType)) {
            throw new EnunciateException("Package " + pckg.getQualifiedName() + ": unadaptable type: " + typeMirror);
          }

          TypeElement typeDeclaration = (TypeElement) ((DeclaredType) typeMirror).asElement();
          if (typeDeclaration == null) {
            throw new EnunciateException("Element not found: " + typeMirror);
          }

          adaptersOfPackage.put(typeDeclaration.getQualifiedName().toString(), adaptedTypeInfo);
        }
      }
    }

    return adaptersOfPackage;
  }
}
