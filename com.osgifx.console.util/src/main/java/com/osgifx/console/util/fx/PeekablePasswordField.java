/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.util.fx;

import org.osgi.framework.FrameworkUtil;

import javafx.scene.Cursor;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.input.MouseEvent;

/**
 * A password field that supports unmasking ("peeking") the typed password
 */
public final class PeekablePasswordField extends PasswordField {

    public PeekablePasswordField() {
        setSkin(new PeekablePasswordFieldSkin(this));
    }

    /**
     * Skin for {@link PeekablePasswordField}; is uses CSS to show an icon on the
     * right side
     */
    class PeekablePasswordFieldSkin extends TextFieldSkin {
        private boolean _doMask = true;

        public PeekablePasswordFieldSkin(final TextField textField) {
            super(textField);
            if (textField instanceof PasswordField) {
                final var bundle = FrameworkUtil.getBundle(getClass());
                textField.getStylesheets().add(bundle.getResource("/peekablepasswordfield.css").toExternalForm());
                textField.getStyleClass().addAll("withicon", "showing");

                textField.setOnMousePressed(event -> {
                    final var vIconsLeftMargin = textField.getWidth() - 20;
                    final var vDoMask          = event.getX() <= vIconsLeftMargin;
                    setDoMask(vDoMask);
                    setIcon(vDoMask);
                });

                textField.setOnMouseReleased(event -> {
                    if (!_doMask) {
                        setDoMask(true);
                        setIcon(true);
                    }
                });

                textField.setOnMouseMoved((final MouseEvent event) -> {
                    final var vIconsLeftMargin = textField.getWidth() - 20;
                    if (event.getX() > vIconsLeftMargin) {
                        textField.setCursor(Cursor.HAND);
                    } else {
                        textField.setCursor(Cursor.TEXT);
                    }
                });

                textField.setOnMouseExited((final MouseEvent event) -> {
                    setDoMask(true);
                    setIcon(true);
                });
            }
        }

        /**
         * Allows switching from "masking" to "unmasking" mode
         *
         * @param pValue if true text must be masked
         */
        private void setDoMask(final boolean pValue) {
            _doMask = pValue;
            final var textField = getSkinnable();
            final var vText     = textField.getText();
            textField.setText(vText);

            setIcon(pValue);
        }

        /**
         * Sets the icon to use (either a simple eye or an eye with a slash)
         *
         * @param pShowing if true show the eye icon without slash
         */
        private void setIcon(final boolean pShowing) {
            final var textField = getSkinnable();
            textField.getStyleClass().removeAll("showing", "hiding");
            if (pShowing) {
                textField.getStyleClass().add("showing");
            } else {
                textField.getStyleClass().add("hiding");
            }
        }

        @Override
        protected String maskText(final String txt) {
            if (_doMask) {
                return super.maskText(txt);
            }
            return txt;
        }

    }

}
