package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble


//import platform.Mo
//import MotoricaStart
//import MotoricaBLEB
//import platform.Foundation.*
//import kotlinx.cinterop.*
//import platform.CoreBluetooth.CBCharacteristic
//import platform.Foundation.NSData
//import platform.Foundation.dataWithBytes


//actual object BleCommandDispatcher {
//    actual fun bleCommandWithQueue(
//        dataForWrite: ByteArray,
//        characteristic: String,
//        type: String
//    ) {
//        val nsData = dataForWrite.toNSData()
////
////        // из Swift-кода:  @objc func characteristic(uuid: String) -> CBCharacteristic?
////        val cbChar: CBCharacteristic = BluetoothRepositoryImpl.shared.characteristic(uuid = characteristic) ?: return
////
////        BluetoothRepositoryImpl.shared.addCommandToQueue(
////            dataForWrite = nsData,
////            characteristic = cbChar,
////            type = type
////        )
//    }
//}

//actual object BleCommandDispatcher {
//    actual fun bleCommandWithQueue(
//        // Вызов функции из вашего существующего класса BluetoothRepositoryImpl
////        val bluetoothRepo = BluetoothRepositoryImpl()
////        bluetoothRepo.connectToDevice(deviceId)
//    }
//}


//@ExperimentalForeignApi
//private fun ByteArray.toNSData(): NSData =
//    usePinned { pinned ->
//        NSData.dataWithBytes(
//            bytes  = pinned.addressOf(0),
//            length = size.toULong()
//        )
//    }