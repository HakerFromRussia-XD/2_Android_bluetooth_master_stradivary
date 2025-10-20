//
//  LoadingContainerView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 16.10.2025.
//

import UIKit
import Lottie

final class LoadingContainerView: UIView {

    private enum AnimationAsset {
        case json(name: String)
        case dotLottie(name: String)
    }
    
    private let contentView = UIView()
    private let stackView = UIStackView()
    private let animationView: LottieAnimationView
    private let messageLabel = UILabel()
    private let progressView = UIProgressView(progressViewStyle: .default)
    private let animationAsset: AnimationAsset
    private var shouldPlayAfterLoading = false
    private var isLoadingAnimation = false

    init(frame: CGRect, animationName: String) {
        if Bundle.main.url(forResource: animationName, withExtension: "lottie") != nil {
            animationAsset = .dotLottie(name: animationName)
        } else {
            animationAsset = .json(name: animationName)
        }
        animationView = LottieAnimationView(configuration: .shared)
        super.init(frame: frame)
        setupView()
        loadAnimationIfNeeded()
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
        guard animationView.isAnimationPlaying == false else { return }
        
        if animationView.animation == nil {
            shouldPlayAfterLoading = true
            loadAnimationIfNeeded()
            return
        }
        animationView.loopMode = .loop
        animationView.play()
        shouldPlayAfterLoading = false
    }

    func stopAnimation() {
        shouldPlayAfterLoading = false
        animationView.stop()
    }

    private func setupView() {
//        backgroundColor = UIColor.black.withAlphaComponent(0.0)
        backgroundColor = UIColor(named: "ubi4_back") ?? UIColor(red: 42.0/255.0, green: 42.0/255.0, blue: 42.0/255.0, alpha: 1.0)
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
        contentView.backgroundColor = UIColor.systemBackground.withAlphaComponent(0.0)
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
            animationView.heightAnchor.constraint(equalToConstant: 240)
        ])
    }

    private func setupMessageLabel() {
        messageLabel.translatesAutoresizingMaskIntoConstraints = false
//        messageLabel.font = .preferredFont(forTextStyle: .body)
//        messageLabel.font = UIFont(name: "OpenSans-wght", size: 8)
        messageLabel.font = UIFont(name: "SFProDisplay-Light", size: 16)
        messageLabel.textAlignment = .center
        messageLabel.textColor = .label
        messageLabel.numberOfLines = 0
        stackView.addArrangedSubview(messageLabel)
    }

    private func setupProgressView() {
        progressView.translatesAutoresizingMaskIntoConstraints = false
        progressView.progress = 0
        progressView.trackTintColor = UIColor.secondarySystemFill
//        progressView.progressTintColor = UIColor.systemBlue
        progressView.progressTintColor = UIColor(named: "ubi4_active") ?? UIColor.systemBlue
        progressView.accessibilityIdentifier = "loading.progress"
        stackView.addArrangedSubview(progressView)
    }
    
    private func loadAnimationIfNeeded() {
        switch animationAsset {
        case .json(let name):
            loadAnimationFromJSON(named: name)
        case .dotLottie(let name):
            loadAnimationFromDotLottie(named: name)
        }
    }

    private func loadAnimationFromJSON(named name: String) {
        guard animationView.animation == nil else { return }

        if let animation = LottieAnimation.named(name) {
            animationView.animation = animation
            animationView.imageProvider = BundleImageProvider(bundle: .main, searchPath: nil)
            if shouldPlayAfterLoading {
                startAnimation()
            }
        }
    }

    private func loadAnimationFromDotLottie(named name: String) {
        guard animationView.animation == nil, isLoadingAnimation == false else {
            return
        }

        isLoadingAnimation = true

        DotLottieFile.named(name, bundle: .main) { [weak self] result in
            guard let self else { return }

            self.isLoadingAnimation = false

            switch result {
            case .success(let dotLottieFile):
                guard let animationContainer = dotLottieFile.animations.first else {
                    self.loadAnimationFromJSON(named: name)
                    return
                }

                self.animationView.animation = animationContainer.animation
                if let provider = animationContainer.configuration.imageProvider {
                    self.animationView.imageProvider = provider
                }
                self.animationView.loopMode = animationContainer.configuration.loopMode
                self.animationView.animationSpeed = CGFloat(animationContainer.configuration.speed)

            case .failure:
                self.loadAnimationFromJSON(named: name)
            }

            if self.shouldPlayAfterLoading {
                self.startAnimation()
            }
        }
    }
}
