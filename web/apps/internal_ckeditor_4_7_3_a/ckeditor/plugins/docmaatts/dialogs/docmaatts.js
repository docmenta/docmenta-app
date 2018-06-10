CKEDITOR.dialog.add( 'docmaattsDialog', function( editor ) {
    return {
        title: editor.lang.docmaatts.attsDialogTitle,
        minWidth: 400,
        minHeight: 200,
        contents: [
            {
                id: 'tab-basic',
                label: editor.lang.docmaatts.baseTabTitle,
                elements: [
                    {
                        type: 'html',
                        id: 'elem_path',
                        html: '',
                        // Called by the main setupContent method call on dialog initialization.
                        setup: function( element ) {
                            var ename = "";
                            var path = "";
                            if (element != null) {
                                ename = element.getName();
                                var par = element.getParent();
                                while (par != null) {
                                    var nm = par.getName();
                                    if ((nm == 'body') || (nm == 'html')) {
                                        break;
                                    }
                                    path = par.getName() + ' &rarr; ' + path;
                                    par = par.getParent();
                                }
                            }
                            this.getElement().setHtml( '<strong>' + editor.lang.docmaatts.elementLabel + ':</strong> ' + path + '<strong>' + ename + '</strong>' );
                        }
                    },
                    {
                        type: 'text',
                        id: 'elem_id',
                        label: editor.lang.docmaatts.idLabel,

                        // validate: CKEDITOR.dialog.validate.notEmpty( "Abbreviation field cannot be empty." )

                        // Called by the main setupContent method call on dialog initialization.
                        setup: function( element ) {
                            var val = (element != null) ? element.getAttribute("id") : "";
                            this.setValue( val );
                        },

                        // Called by the main commitContent method call on dialog confirmation.
                        commit: function( element ) {
                            var val = this.getValue();
                            if (val != "") {
                                element.setAttribute( "id", val );
                            } else {
                                element.removeAttribute( "id" );
                            }
                        }
                    },
                    {
                        type: 'text',
                        id: 'elem_title',
                        label: editor.lang.docmaatts.titleLabel,

                        // validate: CKEDITOR.dialog.validate.notEmpty( "Explanation field cannot be empty." )

                        // Called by the main setupContent method call on dialog initialization.
                        setup: function( element ) {
                            var val = (element != null) ? element.getAttribute("title") : "";
                            this.setValue( val );
                        },

                        // Called by the main commitContent method call on dialog confirmation.
                        commit: function( element ) {
                            var val = this.getValue();
                            if (val != "") {
                                element.setAttribute( "title", val );
                            } else {
                                element.removeAttribute( "title" );
                            }
                        }
                    },
                    {
                        type: 'hbox',
                        widths: [ '65%', '35%' ],
                        children: [ 
                        {
                            type: 'text',
                            id: 'elem_style_class',
                            label: editor.lang.docmaatts.styleIdsLabel,
                            'default': '',
                            setup: function( element ) {
                                var val = (element != null) ? element.getAttribute("class") : "";
                                if (val == null) val = "";
                                var p = val.indexOf('cke_table-faked-selection-table');
                                if (p >= 0) {
                                    val = val.substring(0, p).trim();
                                }
                                this.setValue( val );
                            },

                            // Called by the main commitContent method call on dialog confirmation.
                            commit: function( element ) {
                                var val = this.getValue().trim();
                                if (val != "") {
                                    element.setAttribute( "class", val );
                                } else {
                                    element.removeAttribute( "class" );
                                }
                            }
                        },
                        {
                            type: 'select',
                            id: 'elem_style_select',
                            label: ' ',
                            items: [], // getDocmaClassSelection(),
                            'default': '',
                            onShow: function() {
                                setupClassOptions(this, getDocmaClassSelection());
                                this.setValue('', true);
                            },
                            onChange: function( evt ) {
                                var clsField = this.getDialog().getContentElement( 'tab-basic', 'elem_style_class' );
                                var clsVal = clsField.getValue().trim();
                                var clsVal2 = ' ' + clsVal + ' ';
                                var sel = this.getValue();
                                if (sel != '') {
                                    if (clsVal2.indexOf(' ' + sel + ' ') < 0) {
                                        clsField.setValue((clsVal + ' ' + sel).trim(), true);
                                    }
                                }
                                this.setValue('', true);
                            }
                        }]
                    },
                    {
                        type: 'text',
                        id: 'elem_style_css',
                        label: editor.lang.docmaatts.inlineStyleLabel,

                        // validate: CKEDITOR.dialog.validate.notEmpty( "Explanation field cannot be empty." )

                        // Called by the main setupContent method call on dialog initialization.
                        setup: function( element ) {
                            var val = (element != null) ? element.getAttribute("style") : "";
                            this.setValue( val );
                        },

                        // Called by the main commitContent method call on dialog confirmation.
                        commit: function( element ) {
                            var val = this.getValue().trim();
                            if (val != "") {
                                element.setAttribute( "style", val );
                            } else {
                                element.removeAttribute( "style" );
                            }
                        }
                    }

                ]
            }
            // ,{
            //  id: 'tab-adv',
            //  label: 'Advanced Attributes',
            //  elements: [
            //  ]
            // }
        ],

        // Invoked when the dialog is loaded.
        onShow: function() {

            // Get the selection from the editor.
            var selection = editor.getSelection();

            // Get the element at the start of the selection.
            var element = selection.getSelectedElement();
            if (element == null) {
                element = selection.getStartElement();
            }

            // Store the reference to the element in an internal property, for later use.
            this.element = element;

            // Invoke the setup methods of all dialog window elements, so they can load the element attributes.
            if ( element ) {
                this.setupContent( this.element );
            }
            // else {
                // var notification1 = new CKEDITOR.plugins.notification( editor, {
                //    message: 'Missing element context!',
                //     type: 'warning'
                // } );
                // notification1.show();
            // }

        },

        onOk: function() {
            if (this.element) {
                // Invoke the commit methods of all dialog window elements, so the element gets modified.
                this.commitContent( this.element );
            }
        }
    };
});