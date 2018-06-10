CKEDITOR.plugins.add( 'docmalink', {
    icons: 'docmalink',
    lang: ['en'],
    init: function( editor ) {
        editor.addCommand( 'docmalink', new CKEDITOR.dialogCommand( 'docmalinkDialog' ) );
        editor.addCommand( 'unlink', new CKEDITOR.unlinkCommand() );
        
        editor.setKeystroke( CKEDITOR.CTRL + 76, 'docmalink' );  // set Ctrl+L to link dialog
        
        editor.ui.addButton( 'DocmaLink', {
            label: editor.lang.docmalink.linkBtnLabel,
            // icons: 'docmalink',
            command: 'docmalink',
            toolbar: 'links'
        });
        editor.ui.addButton( 'Unlink', {
            label: editor.lang.link.unlink,
            command: 'unlink',
            toolbar: 'links'
        });
        
        CKEDITOR.dialog.add( 'docmalinkDialog', this.path + 'dialogs/docmalink.js' );

        editor.on( 'doubleclick', function( evt ) {
            var element = evt.data.element.getAscendant( { a: 1, img: 1 }, true );
            if ( element && !element.isReadOnly() ) {
                if ( element.is( 'a' ) ) {
                    evt.data.dialog = 'docmalinkDialog';
                    evt.data.link = element;
                }
            }
        }, null, null, 0 );
        
        // If event was cancelled, link passed in event data will not be selected.
        editor.on( 'doubleclick', function( evt ) {
            // Make sure both links and anchors are selected (https://dev.ckeditor.com/ticket/11822).
            if ( evt.data.dialog in { link: 1, anchor: 1 } && evt.data.link )
                editor.getSelection().selectElement( evt.data.link );
        }, null, null, 20 );
        
        // If the "menu" plugin is loaded, register the menu items.
        if ( editor.addMenuItems ) {
            editor.addMenuItems( {

                link: {
                    label: editor.lang.link.menu,
                    command: 'docmalink',
                    group: 'link',
                    order: 1
                },

                unlink: {
                    label: editor.lang.link.unlink,
                    command: 'unlink',
                    group: 'link',
                    order: 5
                }
            } );
        }

        // If the "contextmenu" plugin is loaded, register the listeners.
        if ( editor.contextMenu ) {
            editor.contextMenu.addListener( function( element ) {
                if ( !element || element.isReadOnly() )
                    return null;

                var anchor = CKEDITOR.plugins.link.tryRestoreFakeAnchor( editor, element );

                if ( !anchor && !( anchor = CKEDITOR.plugins.link.getSelectedLink( editor ) ) )
                    return null;

                var menu = {};

                if ( anchor.getAttribute( 'href' ) && anchor.getChildCount() )
                    menu = { link: CKEDITOR.TRISTATE_OFF, unlink: CKEDITOR.TRISTATE_OFF };

                if ( anchor && anchor.hasAttribute( 'name' ) )
                    menu.anchor = menu.removeAnchor = CKEDITOR.TRISTATE_OFF;

                return menu;
            } );
        }

    }
});