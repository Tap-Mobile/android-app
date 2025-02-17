/*
 * Copyright (c) 2020 Proton Technologies AG
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
package com.protonvpn.android.models.vpn

import android.content.Context
import com.protonvpn.android.appconfig.AppConfig
import com.protonvpn.android.models.config.TransmissionProtocol
import com.protonvpn.android.models.config.UserData
import com.protonvpn.android.models.config.VpnProtocol
import com.protonvpn.android.models.profiles.Profile
import com.protonvpn.android.utils.Constants
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.Connection

class ConnectionParamsOpenVpn(
    profile: Profile,
    server: Server,
    connectingDomain: ConnectingDomain,
    private val transmissionProtocol: TransmissionProtocol,
    private val port: Int
) : ConnectionParams(profile, server, connectingDomain, VpnProtocol.OpenVPN), java.io.Serializable {

    override val info get() = "${super.info} $transmissionProtocol port=$port"
    override val transmission get() = transmissionProtocol

    fun openVpnProfile(
        context: Context,
        userData: UserData,
        appConfig: AppConfig
    ) = VpnProfile(server.getLabel(context)).apply {
        mAuthenticationType = VpnProfile.TYPE_USERPASS
        mCaFilename = Constants.VPN_ROOT_CERTS
        mTLSAuthFilename = TLS_AUTH_KEY
        mTLSAuthDirection = "1"
        mAuth = "SHA512"
        mCipher = "AES-256-CBC"
        mUsername = getVpnUsername(userData, appConfig)
        mUseTLSAuth = true
        mTunMtu = 1500
        mMssFix = userData.mtuSize - 40
        mExpectTLSCert = true
        mX509AuthType = VpnProfile.X509_VERIFY_TLSREMOTE_SAN
        mCheckRemoteCN = true
        mRemoteCN = connectingDomain!!.entryDomain
        mPersistTun = true
        mAllowLocalLAN = userData.bypassLocalTraffic()
        if (userData.useSplitTunneling && userData.splitTunnelIpAddresses.isNotEmpty()) {
            mUseDefaultRoute = false
            mExcludedRoutes = userData.splitTunnelIpAddresses.joinToString(" ")
        }
        mConnections[0] = Connection().apply {
            if (userData.useSplitTunneling)
                mAllowedAppsVpn = HashSet<String>(userData.splitTunnelApps)
            mServerName = connectingDomain.entryIp
            mUseUdp = transmissionProtocol == TransmissionProtocol.UDP
            mServerPort = port.toString()
            mCustomConfiguration = ""
        }
        mPassword = userData.vpnPassword
    }

    override fun hasSameProtocolParams(other: ConnectionParams) =
        other is ConnectionParamsOpenVpn && other.transmissionProtocol == transmissionProtocol && other.port == port

    companion object {

        const val TLS_AUTH_KEY =
            "[[INLINE]]# 2048 bit OpenVPN static key\n" +
            "-----BEGIN OpenVPN Static key V1-----\n" +
            Constants.TLS_AUTH_KEY_HEX +
            "-----END OpenVPN Static key V1-----"
    }
}
