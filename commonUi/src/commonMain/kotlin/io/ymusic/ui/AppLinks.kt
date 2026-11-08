package io.ymusic.ui

import io.ymusic.remoteconfig.RemoteConfigData

/** The app's links and contact address: the remote config's value when it sends one, else the built-in one. */
internal class AppLinks(config: RemoteConfigData?) {
    val website: String = config?.features?.websiteUrl ?: "https://github.com/ElionKrasniqi0/StartMusicV3"
    val telegram: String = config?.features?.telegramUrl ?: "https://github.com/ElionKrasniqi0/StartMusicV3"
    val feedbackEmail: String = config?.features?.feedbackEmail ?: "feedback@example.com"
    val feedbackIssue: String = config?.features?.feedbackIssueUrl
        ?: "https://github.com/ElionKrasniqi0/StartMusicV3/issues/new"
    val sourceCode: String = config?.features?.sourceCodeUrl
        ?: "https://github.com/ElionKrasniqi0/StartMusicV3"
    val terms: String = config?.features?.termsUrl
        ?: "https://github.com/ElionKrasniqi0/StartMusicV3/blob/master/TERMS.md"
    val changelog: String = config?.features?.changelogUrl
        ?: "https://github.com/ElionKrasniqi0/StartMusicV3/blob/master/CHANGELOG.md"
}
