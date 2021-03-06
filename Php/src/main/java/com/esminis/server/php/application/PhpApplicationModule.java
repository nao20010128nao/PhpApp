package com.esminis.server.php.application;

import com.esminis.server.library.activity.DrawerFragment;
import com.esminis.server.library.activity.main.MainPresenter;
import com.esminis.server.library.activity.main.MainPresenterImpl;
import com.esminis.server.library.application.*;
import com.esminis.server.library.model.manager.InstallPackageManager;
import com.esminis.server.library.model.manager.Log;
import com.esminis.server.library.model.manager.Network;
import com.esminis.server.library.preferences.Preferences;
import com.esminis.server.library.service.server.ServerControl;
import com.esminis.server.library.service.server.ServerNotification;
import com.esminis.server.library.service.server.installpackage.InstallerPackage;
import com.esminis.server.php.activity.DrawerPhpFragment;
import com.esminis.server.php.server.Php;
import com.esminis.server.php.server.install.InstallToDocumentRoot;
import com.esminis.server.php.server.install.InstallerPackagePhp;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class PhpApplicationModule {

	private final LibraryApplication application;

	public PhpApplicationModule(LibraryApplication application) {
		this.application = application;
	}

	@Provides
	@Singleton
	public ServerControl provideServerControl(
		Network network, com.esminis.server.library.model.manager.Process process, Log log,
		Preferences preferences, ServerNotification serverNotification
	) {
		return new Php(network, process, preferences, log, application, serverNotification);
	}

	@Provides
	@Singleton
	public InstallerPackage provideInstallerPackage(
		ServerControl control, InstallToDocumentRoot installToDocumentRoot,
		InstallPackageManager installPackageManager, Preferences preferences
	) {
		return new InstallerPackagePhp(
			control, installToDocumentRoot, installPackageManager, preferences
		);
	}

	@Provides
	public DrawerFragment provideDrawerFragment(DrawerPhpFragment implementation) {
		return implementation;
	}

	@Provides
	public MainPresenter provideMainPresenter(MainPresenterImpl implementation) {
		return implementation;
	}

}
