package org.gamboni.shopping.server.ui;

import jakarta.enterprise.context.ApplicationScoped;
import org.gamboni.shopping.server.domain.State;
import org.gamboni.tech.web.ui.Css;

/**
 * @author tendays
 */
@ApplicationScoped
public class Style extends Css {
    public OneCssClassPerEnumValue<State> forState = new OneCssClassPerEnumValue<>(State.class);
    public ClassName text = new ClassName("text");
    public ClassName image = new ClassName("image");

    public String render() {
        Properties a = Properties.INSTANCE;
        return rule(forState.valueStream().reduce(Selector.NOTHING, Selector::or, Selector::or),
                a.width("300px"),
                a.height("300px"),
                CssFloat.LEFT,
                Position.RELATIVE
        ) +
                rule(text,
                        Position.ABSOLUTE,
                        a.width("auto"),
                        a.backgroundColor("white"),
                        a.top("0"),
                        a.borderBottomRightRadius("5px"),
                        a.padding("3px")
                ) +
                rule(forState.get(State.UNUSED).child(image),
                        a.filter("grayscale(100%) blur(1px)")
                ) +
                rule(forState.get(State.TO_BUY).child(text),
                        a.fontWeight("bold"),
                        a.backgroundColor("orange"),
                        a.color("white")) +
                rule(forState.get(State.BOUGHT).before(),
                        a.content("✅"), // note: "checkmark" emoji between quotes
                        Position.ABSOLUTE,
                        a.right("0"),
                        a.top("0"),
                        a.fontSize("3em"));
    }
}
