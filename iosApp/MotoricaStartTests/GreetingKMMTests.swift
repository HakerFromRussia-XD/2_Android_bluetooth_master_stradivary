//
//  GreetingKMMTests.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 15.05.2025.
//
// Assistant: простой проверки, что KMM-фреймворк подключён и доступен
import XCTest
//import Shared            // KMM .xcframework

final class GreetingKMMTests: XCTestCase {

    func testGreeting() {
        // act
        let text = Greeting().greeting()

        // assert
        XCTAssertEqual(text, "Hello, iOS!")
    }
//    let a = 
}

