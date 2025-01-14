/*
 * Copyright (c) 2019 Proton Technologies AG
 * 
 * This file is part of ProtonVPN.
 * 
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.protonvpn.android.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.protonvpn.android.tv.detailed.TvServerListViewModel
import com.protonvpn.android.tv.login.TvLoginViewModel
import com.protonvpn.android.tv.main.MainViewModel
import com.protonvpn.android.tv.main.TvMainViewModel
import com.protonvpn.android.ui.drawer.AccountActivityViewModel
import com.protonvpn.android.ui.home.InformationViewModel
import com.protonvpn.android.ui.home.countries.CountryListViewModel
import com.protonvpn.android.ui.home.profiles.HomeViewModel
import com.protonvpn.android.ui.home.profiles.ProfileViewModel
import com.protonvpn.android.ui.home.profiles.ProfilesViewModel
import com.protonvpn.android.ui.login.LoginViewModel
import com.protonvpn.android.ui.login.TroubleshootViewModel
import com.protonvpn.android.ui.promooffers.PromoOfferNotificationViewModel
import com.protonvpn.android.ui.promooffers.PromoOfferViewModel
import com.protonvpn.android.utils.ViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(CountryListViewModel::class)
    abstract fun bindsCountryListViewModel(yourViewModel: CountryListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfilesViewModel::class)
    abstract fun bindsProfilesViewModel(profilesViewModel: ProfilesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    abstract fun bindsLoginViewModel(loginViewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(InformationViewModel::class)
    abstract fun bindsInformationViewModel(informationViewModel: InformationViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindsProfileViewModel(profileViewModel: ProfileViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindsHomeViewModel(viewModel: HomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TvMainViewModel::class)
    abstract fun bindsTvHomeViewModel(viewModel: TvMainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TvServerListViewModel::class)
    abstract fun bindsTvServerListViewModel(viewModel: TvServerListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindsMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TvLoginViewModel::class)
    abstract fun bindsTvLoginViewModel(viewModel: TvLoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TroubleshootViewModel::class)
    abstract fun bindsTroubleshootViewModel(viewModel: TroubleshootViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AccountActivityViewModel::class)
    abstract fun bindsAccountActivityViewModel(viewModel: AccountActivityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PromoOfferViewModel::class)
    abstract fun bindsPromoOfferViewModel(viewModel: PromoOfferViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PromoOfferNotificationViewModel::class)
    abstract fun bindsPromoOfferNotificationVM(viewModel: PromoOfferNotificationViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(vmFactory: ViewModelFactory): ViewModelProvider.Factory
}
