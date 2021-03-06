package de.plushnikov.intellij.plugin.processor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import de.plushnikov.intellij.plugin.lombokconfig.ConfigDiscovery;
import de.plushnikov.intellij.plugin.lombokconfig.ConfigKeys;
import de.plushnikov.intellij.plugin.processor.field.AccessorsInfo;
import de.plushnikov.intellij.plugin.thirdparty.LombokUtils;
import de.plushnikov.intellij.plugin.util.PsiAnnotationUtil;
import lombok.experimental.Tolerate;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Base lombok processor class
 *
 * @author Plushnikov Michail
 */
public abstract class AbstractProcessor implements Processor {
  /**
   * Anntotation qualified name this processor supports
   */
  private final String supportedAnnotation;
  /**
   * Anntotation class this processor supports
   */
  private final Class<? extends Annotation> supportedAnnotationClass;
  /**
   * Kind of output elements this processor supports
   */
  private final Class<? extends PsiElement> supportedClass;

  /**
   * Constructor for all Lombok-Processors
   *
   * @param supportedAnnotationClass annotation this processor supports
   * @param supportedClass           kind of output elements this processor supports
   */
  protected AbstractProcessor(@NotNull Class<? extends Annotation> supportedAnnotationClass, @NotNull Class<? extends PsiElement> supportedClass) {
    this.supportedAnnotationClass = supportedAnnotationClass;
    this.supportedAnnotation = supportedAnnotationClass.getName();
    this.supportedClass = supportedClass;
  }

  @NotNull
  @Override
  public final String getSupportedAnnotation() {
    return supportedAnnotation;
  }

  @NotNull
  @Override
  public final Class<? extends Annotation> getSupportedAnnotationClass() {
    return supportedAnnotationClass;
  }

  @NotNull
  @Override
  public final Class<? extends PsiElement> getSupportedClass() {
    return supportedClass;
  }

  public boolean acceptAnnotation(@NotNull PsiAnnotation psiAnnotation, @NotNull Class<? extends PsiElement> type) {
    final String annotationName = StringUtil.notNullize(psiAnnotation.getQualifiedName()).trim();
    return supportedAnnotation.equals(annotationName) && canProduce(type);
  }

  @Override
  public boolean isEnabled(@NotNull Project project) {
    return true;//TODO make it configurable
  }

  @Override
  public boolean canProduce(@NotNull Class<? extends PsiElement> type) {
    return type.isAssignableFrom(supportedClass);
  }

  @NotNull
  public List<? super PsiElement> process(@NotNull PsiClass psiClass) {
    return Collections.emptyList();
  }

  @NotNull
  public abstract Collection<PsiAnnotation> collectProcessedAnnotations(@NotNull PsiClass psiClass);

  protected String getGetterName(final @NotNull PsiField psiField, final @NotNull PsiClass psiClass) {
    final AccessorsInfo accessorsInfo = AccessorsInfo.build(psiField);

    final String psiFieldName = psiField.getName();
    final boolean isBoolean = PsiType.BOOLEAN.equals(psiField.getType());

    return getGetterName(accessorsInfo, psiFieldName, isBoolean, psiClass);
  }

  public String getGetterName(AccessorsInfo accessorsInfo, String psiFieldName, boolean isBoolean, PsiClass psiClass) {
    final String fieldNameWithoutPrefix = accessorsInfo.removePrefix(psiFieldName);
    if (accessorsInfo.isFluent()) {
      return LombokUtils.decapitalize(fieldNameWithoutPrefix);
    }

    final boolean useBooleanPrefix = isBoolean && !ConfigDiscovery.getInstance().getBooleanLombokConfigProperty(ConfigKeys.GETTER_NO_IS_PREFIX, psiClass);

    return LombokUtils.toGetterName(fieldNameWithoutPrefix, useBooleanPrefix);
  }

  protected void filterToleratedElements(@NotNull Collection<? extends PsiModifierListOwner> definedConstructors) {
    final Iterator<? extends PsiModifierListOwner> methodIterator = definedConstructors.iterator();
    while (methodIterator.hasNext()) {
      PsiModifierListOwner definedConstructor = methodIterator.next();
      if (PsiAnnotationUtil.isAnnotatedWith(definedConstructor, Tolerate.class)) {
        methodIterator.remove();
      }
    }
  }

  public static boolean readAnnotationOrConfigProperty(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass,
                                                       @NotNull String annotationParameter, @NotNull ConfigKeys configKeys) {
    final boolean result;
    final Boolean declaredAnnotationValue = PsiAnnotationUtil.getDeclaredAnnotationValue(psiAnnotation, annotationParameter, Boolean.class);
    if (null == declaredAnnotationValue) {
      result = ConfigDiscovery.getInstance().getBooleanLombokConfigProperty(configKeys, psiClass);
    } else {
      result = declaredAnnotationValue;
    }
    return result;
  }
}
