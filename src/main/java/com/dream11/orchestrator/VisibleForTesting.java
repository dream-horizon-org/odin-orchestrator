package com.dream11.orchestrator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ref -
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:annotation/annotation/src/commonMain/kotlin/androidx/annotation/VisibleForTesting.kt?q=file:androidx%2Fannotation%2FVisibleForTesting.kt%20class:androidx.annotation.VisibleForTesting&ss=androidx%2Fplatform%2Fframeworks%2Fsupport
 * Denotes that the class, method or field has its visibility relaxed, so that it is more widely
 * visible than otherwise necessary to make code testable.
 *
 * <p>You can optionally specify what the visibility **should** have been if not for testing; this
 * allows tools to catch unintended access from within production code.
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface VisibleForTesting {

  int NOT_REQUIRED = 5;
  int PACKAGE_PRIVATE = 2;
  int PRIVATE = 1;
  int PROTECTED = 3;
  int PUBLIC = 4;

  int otherwise();
}
