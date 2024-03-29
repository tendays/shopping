package org.gamboni.shopping.server.ui;

import org.gamboni.shopping.server.domain.State;
import org.gamboni.shopping.server.tech.ui.Css;

/**
 * @author tendays
 */
public class Style extends Css {
    public EnumToClassName<State> forState = new EnumToClassName<>(State.class);
    public ClassName text = new ClassName("text");
    public ClassName image = new ClassName("image");

    public String render() {
        Properties a = Properties.INSTANCE;
        return rule(forState.valueStream().reduce(Selector.NOTHING, Selector::or, Selector::or),
                a.width("300px"),
                a.height("300px"),
                a._float("left"),
                a.position("relative")
        ) +
                rule(text,
                        a.position("absolute"),
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
                        a.content("\"✅\""), // note: "checkmark" emoji between quotes
                        a.position("absolute"),
                        a.right("0"),
                        a.top("0"),
                        a.fontSize("3em"));
    }
}
