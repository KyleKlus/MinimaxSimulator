package de.uni_hannover.sra.minimax_simulator.ui.gui.components.dialogs;

import de.uni_hannover.sra.minimax_simulator.Main;
import de.uni_hannover.sra.minimax_simulator.model.machine.simulation.Traceable;
import de.uni_hannover.sra.minimax_simulator.resources.TextResource;

/**
 * The {@code RegisterUpdateDialog} is basically an {@link ValueUpdateDialog}.<br>
 * It prompts for which register the value will be changed. On confirmation the new value will be stored.
 *
 * @author Philipp Rohde
 */
public class RegisterUpdateDialog extends ValueUpdateDialog {

    private final Traceable<Integer> value;

    /**
     * Constructs a new {@code RegisterUpdateDialog} for the register with the specified name
     * and the specified value.
     *
     * @param register
     *          the name of the register
     * @param value
     *          the {@link Traceable} value of the register
     */
    public RegisterUpdateDialog(String register, Traceable<Integer> value) {
        super(value.get(), true);

        this.value = value;

        TextResource res = Main.getTextResource("debugger").using("register.update");

        messageText.setText(res.format("message", register));
    }

    /**
     * Sets the new value to the register for which the dialog was opened.
     *
     * @param value
     *          the new value
     */
    @Override
    protected void setValue(int value) {
        this.value.set(value);
    }
}