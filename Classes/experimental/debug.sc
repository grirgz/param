
//////////////////////// Debug

+Window {
	*keyDownActionTest {
		var window = Window.new;
		var layout;
		var field = TextField.new;
		window.view.keyDownAction = { arg me, key, modifiers, unicode, keycode;
			[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
			field.string = [me, key, modifiers, unicode, keycode].asCompileString;
			true;
		};
		field.keyDownAction = window.view.keyDownAction;
		layout = VLayout(
			field
		);
		window.layout = layout;
		window.alwaysOnTop = true;
		window.front;

	}
}
