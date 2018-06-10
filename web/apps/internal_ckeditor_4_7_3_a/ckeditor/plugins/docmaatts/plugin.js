CKEDITOR.plugins.add( 'docmaatts', {
    icons: 'docmaatts',
    lang: ['en'],
    init: function( editor ) {
        editor.addCommand( 'docmaatts', new CKEDITOR.dialogCommand( 'docmaattsDialog' ) );
        editor.ui.addButton( 'Docmaatts', {
            label: editor.lang.docmaatts.attsBtnLabel,
            command: 'docmaatts',
            toolbar: 'insert'
        });

        CKEDITOR.dialog.add( 'docmaattsDialog', this.path + 'dialogs/docmaatts.js' );

        // If the "menu" plugin is loaded, register the menu items.
        if ( editor.addMenuItems ) {
            editor.addMenuItems( {
                docmaatts: {
                    label: editor.lang.docmaatts.attsDialogTitle,
                    command: 'docmaatts',
                    group: 'link',
                    order: 1
                }
            } );
        }

        // If the "contextmenu" plugin is loaded, register the listeners.
        if ( editor.contextMenu ) {
            editor.contextMenu.addListener( function( element ) {
                if ( !element || element.isReadOnly() ) return null;
                var menu = { docmaatts: CKEDITOR.TRISTATE_OFF };
                return menu;
            } );
        }

    }
});