//
//  LoadingContainerView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 16.10.2025.
//

import UIKit
import Lottie

final class LoadingContainerView: UIView {

    private let contentView = UIView()
    private let stackView = UIStackView()
    private let animationView: LottieAnimationView
    private let messageLabel = UILabel()
    private let progressView = UIProgressView(progressViewStyle: .default)
    private let animationName: String

    init(frame: CGRect, animationName: String) {
        self.animationName = animationName
        animationView = LottieAnimationView(name: animationName)
        super.init(frame: frame)
        setupView()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func apply(state: LoadingView.State) {
        messageLabel.text = state.message
        let clampedProgress = max(0, min(1, state.progress))
        let animated = progressView.progress > 0 && clampedProgress >= progressView.progress
        progressView.setProgress(clampedProgress, animated: animated)
    }

    func startAnimation() {
        if animationView.animation == nil {
            animationView.animation = LottieAnimation.named(animationName)
        }
        guard animationView.animation != nil else { return }
        guard animationView.isAnimationPlaying == false else { return }
        animationView.loopMode = .loop
        animationView.play()
    }

    func stopAnimation() {
        animationView.stop()
    }

    private func setupView() {
        backgroundColor = UIColor.black.withAlphaComponent(0.4)
        translatesAutoresizingMaskIntoConstraints = true
        autoresizingMask = [.flexibleWidth, .flexibleHeight]

        setupContentView()
        setupStackView()
        setupAnimationView()
        setupMessageLabel()
        setupProgressView()
    }

    private func setupContentView() {
        contentView.translatesAutoresizingMaskIntoConstraints = false
        contentView.backgroundColor = UIColor.systemBackground.withAlphaComponent(0.92)
        contentView.layer.cornerRadius = 16
        contentView.layer.masksToBounds = true
        addSubview(contentView)

        NSLayoutConstraint.activate([
            contentView.centerXAnchor.constraint(equalTo: centerXAnchor),
            contentView.centerYAnchor.constraint(equalTo: centerYAnchor),
            contentView.leadingAnchor.constraint(greaterThanOrEqualTo: leadingAnchor, constant: 24),
            contentView.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor, constant: -24)
        ])
    }

    private func setupStackView() {
        stackView.axis = .vertical
        stackView.spacing = 16
        stackView.alignment = .fill
        stackView.translatesAutoresizingMaskIntoConstraints = false

        contentView.addSubview(stackView)

        NSLayoutConstraint.activate([
            stackView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 24),
            stackView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            stackView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            stackView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -24)
        ])
    }

    private func setupAnimationView() {
        animationView.translatesAutoresizingMaskIntoConstraints = false
        animationView.contentMode = .scaleAspectFit
        animationView.loopMode = .loop
        animationView.backgroundBehavior = .pauseAndRestore
        stackView.addArrangedSubview(animationView)

        NSLayoutConstraint.activate([
            animationView.heightAnchor.constraint(equalToConstant: 120)
        ])
    }

    private func setupMessageLabel() {
        messageLabel.translatesAutoresizingMaskIntoConstraints = false
        messageLabel.font = .preferredFont(forTextStyle: .body)
        messageLabel.textAlignment = .center
        messageLabel.textColor = .label
        messageLabel.numberOfLines = 0
        stackView.addArrangedSubview(messageLabel)
    }

    private func setupProgressView() {
        progressView.translatesAutoresizingMaskIntoConstraints = false
        progressView.progress = 0
        progressView.trackTintColor = UIColor.secondarySystemFill
        progressView.progressTintColor = UIColor.systemBlue
        progressView.accessibilityIdentifier = "loading.progress"
        stackView.addArrangedSubview(progressView)
    }
}
