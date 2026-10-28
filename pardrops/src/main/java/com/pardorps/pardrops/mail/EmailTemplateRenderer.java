package com.pardorps.pardrops.mail;

import com.pardorps.pardrops.dto.RenderedEmail;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sustituye las variables {@code {{nombre}}} de una plantilla.
 *
 * <p>En el cuerpo HTML los valores se escapan para evitar inyección. Los
 * fragmentos que genera la propia aplicación (por ejemplo, la tabla de
 * productos) se marcan como {@link SafeHtml} y se insertan tal cual. En el
 * asunto no se aplica escape HTML: se eliminan los saltos de línea.</p>
 */
public final class EmailTemplateRenderer {

    private static final Pattern VARIABLE =
            Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private EmailTemplateRenderer() {
    }

    /**
     * Renderiza asunto y cuerpo.
     *
     * @param subject asunto con variables.
     * @param body    cuerpo HTML con variables.
     * @param model   valores disponibles.
     * @return el correo listo para enviar.
     */
    public static RenderedEmail render(String subject, String body, Map<String, ?> model) {
        return new RenderedEmail(
                substitute(subject, model, false),
                substitute(body, model, true));
    }

    /**
     * Sustituye las variables de una cadena.
     *
     * @param template plantilla.
     * @param model    valores.
     * @param html     {@code true} para escapar los valores como HTML.
     * @return la cadena resultante.
     */
    private static String substitute(String template, Map<String, ?> model, boolean html) {
        if (template == null) {
            return "";
        }
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            Object value = model.get(matcher.group(1));
            String replacement;
            if (value == null) {
                replacement = "";
            } else if (value instanceof SafeHtml safe) {
                replacement = safe.value();
            } else {
                String text = String.valueOf(value);
                replacement = html
                        ? HtmlUtils.htmlEscape(text)
                        : text.replace("\r", " ").replace("\n", " ");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Fragmento HTML generado por la aplicación que no debe escaparse.
     *
     * @param value HTML seguro.
     */
    public record SafeHtml(String value) {
    }
}
