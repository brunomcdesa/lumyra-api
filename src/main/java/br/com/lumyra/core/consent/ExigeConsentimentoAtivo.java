package br.com.lumyra.core.consent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um método de endpoint que persiste/coleta dado sensível e exige
 * consentimento LGPD ativo da aluna (G2). O {@link ConsentimentoGuardAspect}
 * barra (403) quando não há consentimento ativo.
 *
 * <p>Reutilizável pelas próximas fases (anamnese, composição, dor, ciclo).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExigeConsentimentoAtivo {
}
