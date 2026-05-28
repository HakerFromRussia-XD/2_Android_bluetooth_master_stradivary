package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.shared.SharedRes
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource

data class InstructionSection(
    val title: StringResource,
    val items: List<InstructionMenuItem>
)

data class InstructionMenuItem(
    val id: String,
    val title: StringResource,
    val actionType: InstructionActionType,
    val target: String
)

enum class InstructionActionType {
    PAGE,
    PHONE,
    URL,
    DISABLED
}

data class InstructionPage(
    val id: String,
    val title: StringResource,
    val cards: List<InstructionCard>,
    val relatedItems: List<InstructionMenuItem>
)

data class InstructionCard(
    val blocks: List<InstructionBlock>
)

data class InstructionBlock(
    val type: InstructionBlockType,
    val text: StringResource?,
    val image: ImageResource?,
    val items: List<StringResource>,
    val imageHeight: Int
)

enum class InstructionBlockType {
    HEADING,
    PARAGRAPH,
    NOTICE,
    NUMBERED,
    BULLETS,
    IMAGE
}

object InstructionBridge {
    private const val PAGE_SENSORS = "sensors"
    private const val PAGE_GESTURES = "gestures"
    private const val PAGE_ADVANCED = "advanced"
    private const val PAGE_HOW_WORKS = "how_works"
    private const val PAGE_SOCKET = "socket"
    private const val PAGE_COMPLETE_SET = "complete_set"
    private const val PAGE_CHARGING = "charging"
    private const val PAGE_CARE = "care"
    private const val PAGE_SERVICE = "service"

    fun indexSections(): List<InstructionSection> = listOf(
        InstructionSection(
            title = SharedRes.strings.app_control,
            items = appControlItems()
        ),
        InstructionSection(
            title = SharedRes.strings.prostheses_use,
            items = prosthesesItems()
        ),
        InstructionSection(
            title = SharedRes.strings.contact_us,
            items = listOf(
                InstructionMenuItem("contact_support", SharedRes.strings.contact_support, InstructionActionType.PHONE, "88007077197"),
                InstructionMenuItem("vk", SharedRes.strings.instruction_vk, InstructionActionType.URL, "https://vk.com/motorica"),
                InstructionMenuItem("telegram", SharedRes.strings.instruction_telegram, InstructionActionType.URL, "https://t.me/motoricans")
            )
        )
    )

    fun page(id: String): InstructionPage? = when (id) {
        PAGE_SENSORS -> sensorsPage()
        PAGE_GESTURES -> gesturesPage()
        PAGE_ADVANCED -> advancedPage()
        PAGE_HOW_WORKS -> howWorksPage()
        PAGE_SOCKET -> socketPage()
        PAGE_COMPLETE_SET -> completeSetPage()
        PAGE_CHARGING -> chargingPage()
        PAGE_CARE -> carePage()
        PAGE_SERVICE -> servicePage()
        else -> null
    }

    private fun appControlItems(): List<InstructionMenuItem> = listOf(
        pageItem(PAGE_SENSORS, SharedRes.strings.sensor_settingss),
        pageItem(PAGE_GESTURES, SharedRes.strings.setting_gestures),
        InstructionMenuItem("training", SharedRes.strings.training, InstructionActionType.DISABLED, ""),
        pageItem(PAGE_ADVANCED, SharedRes.strings.advanced_settings)
    )

    private fun prosthesesItems(): List<InstructionMenuItem> = listOf(
        pageItem(PAGE_HOW_WORKS, SharedRes.strings.how_prostheses_works),
        pageItem(PAGE_SOCKET, SharedRes.strings.how_to_put_on_a_prostheses_socket),
        pageItem(PAGE_COMPLETE_SET, SharedRes.strings.complete_set),
        pageItem(PAGE_CHARGING, SharedRes.strings.prostheses_charge),
        pageItem(PAGE_CARE, SharedRes.strings.prostheses_care),
        pageItem(PAGE_SERVICE, SharedRes.strings.service_warranty)
    )

    private fun pageItem(id: String, title: StringResource): InstructionMenuItem =
        InstructionMenuItem(id, title, InstructionActionType.PAGE, id)

    private fun relatedAppItems(currentId: String): List<InstructionMenuItem> =
        appControlItems().filter { it.id != currentId }

    private fun relatedProsthesesItems(currentId: String): List<InstructionMenuItem> =
        prosthesesItems().filter { it.id != currentId }

    private fun sensorsPage(): InstructionPage = InstructionPage(
        id = PAGE_SENSORS,
        title = SharedRes.strings.sensor_settingss,
        cards = listOf(
            InstructionCard(
                blocks = listOf(
                    heading(SharedRes.strings.sensor_settingss),
                    paragraph(SharedRes.strings.the_prostheses_is_controlled_with_emg_sensors_by_configuring_two_indicators_sensitivity_and_threshold),
                    paragraph(SharedRes.strings.each_sensor_has_its_own_color),
                    image(SharedRes.images.help_image_7, 70),
                    image(SharedRes.images.help_image_8, 70),
                    heading(SharedRes.strings.what_is_sensitivity_responsible_for),
                    paragraph(SharedRes.strings.the_sensitivity_determines_how_much_muscle_tension_is_required_for_the_sensor_to_read_the_signal_decrease_the_sensitivity_if_there_is_a_lot_of_background_noise_and_increase_it_if_you_have_to_strain_the_muscle_too_much_when_controlling_the_prostheses),
                    paragraph(SharedRes.strings.by_dragging_these_sliders_you_can_change_the_sensitivity_levels_of_the_sensors),
                    image(SharedRes.images.ubi4_help_image_9_us, 180),
                    heading(SharedRes.strings.what_is_the_threshold_responsible_for),
                    paragraph(SharedRes.strings.in_order_for_the_prostheses_to_open_or_close_the_signal_from_the_sensor_must_cross_a_threshold_but_this_will_only_happen_if_the_signals_from_the_sensors_do_not_cross_their_thresholds_at_the_same_time_as_each_other),
                    paragraph(SharedRes.strings.the_threshold_levels_can_be_changed_by_moving_these_sliders_up_and_down),
                    image(SharedRes.images.ubi4_help_image_10_us, 180)
                )
            ),
            InstructionCard(
                blocks = listOf(
                    heading(SharedRes.strings.how_to_set_it_up_correctly),
                    numbered(
                        SharedRes.strings.stretch_the_flexor_extensor_muscles_several_times_in_sequence_observing_the_signals_from_the_sensors_on_the_graph,
                        SharedRes.strings.important_the_signals_must_not_overlap,
                        SharedRes.strings.when_performing_there_are_3_possible_types_of_situations_observed_on_the_graphs_before_changing_the_sensor_sensitivity_levels_you_must_understand_what_kind_of_situation_you_are_observing,
                        SharedRes.strings.reduce_to_at_least_five_units_the_sensitivity_of_the_sensor_whose_signal_is_parasitic_for_example_when_trying_to_achieve_an_open_signal_a_parasitic_signal_from_a_close_sensor_would_be_parasitic_until_its_level_is_consistently_below_50,
                        SharedRes.strings.increase_the_sensitivity_of_the_sensor_whose_signal_is_not_visible_on_the_graph,
                        SharedRes.strings.in_this_case_it_is_not_necessary_to_change_the_sensitivity_settings_of_the_sensors_the_sensors_are_set_correctly
                    ),
                    image(SharedRes.images.ubi4_help_image_11, 160),
                    image(SharedRes.images.ubi4_help_image_12, 160),
                    image(SharedRes.images.ubi4_help_image_13, 160)
                )
            )
        ),
        relatedItems = relatedAppItems(PAGE_SENSORS)
    )

    private fun gesturesPage(): InstructionPage = InstructionPage(
        id = PAGE_GESTURES,
        title = SharedRes.strings.setting_gestures,
        cards = listOf(
            InstructionCard(
                blocks = listOf(
                    heading(SharedRes.strings.is_the_prostheses_calibrated),
                    paragraph(SharedRes.strings.by_default_the_prostheses_comes_calibrated_and_with_the_first_gesture_set_you_can_check_if_the_denture_is_correctly_calibrated_by_pressing_the_open_close_buttons),
                    paragraph(SharedRes.strings.when_the_first_gesture_in_the_gesture_menu_is_selected),
                    image(SharedRes.images.ubi4_help_image_gesture_settings_1, 180),
                    paragraph(SharedRes.strings.if_the_prosthetic_hand_closes_into_a_fist_then_it_is_calibrated_correctly_if_it_does_not_close_in_the_fist_or_does_not_move_you_should_contact_the_service_center_for_consultation),
                    image(SharedRes.images.ubi4_help_image_gesture_settings_2, 180),
                    image(SharedRes.images.ubi4_help_image_gesture_settings_3, 180)
                )
            ),
            InstructionCard(
                blocks = listOf(
                    heading(SharedRes.strings.configuring_your_first_personal_gesture),
                    paragraph(SharedRes.strings.after_checking_that_the_calibration_and_the_first_gesture_are_working_you_can_start_setting_up_your_own_gestures_to_do_this_click_on_the_gear_button_next_to_the_name_of_the_gesture_you_want_to_adjust),
                    image(SharedRes.images.ubi4_help_image_gesture_settings_4, 180),
                    paragraph(SharedRes.strings.this_will_open_a_window_in_which_you_will_see_a_3d_model_of_the_prosthetic_hand_in_the_open_state_of_the_customizable_gesture_change_the_grip_by_tapping_any_finger_and_then_swiping_up_or_down_on_the_screen),
                    image(SharedRes.images.ubi4_help_image_gesture_settings_5, 180),
                    paragraph(SharedRes.strings.the_length_of_the_swipe_adjusts_the_angle_of_the_finger_bend),
                    paragraph(SharedRes.strings.when_adjusting_the_forefinger_middle_finger_ring_finger_and_pinky_finger_only_the_swipe_in_the_vertical_direction_is_taken_into_consideration_and_two_axes_are_used_simultaneously_to_adjust_the_position_of_the_thumb),
                    paragraph(SharedRes.strings.the_vertical_axis_control_the_angle_of_flexion_of_the_thumb_itself_and_the_horizontal_axis_control_the_angle_of_rotation_of_the_thumb),
                    paragraph(SharedRes.strings.swipes_outside_the_hand_rotate_it_in_space),
                    image(SharedRes.images.ubi4_help_image_gesture_settings_6, 180),
                    paragraph(SharedRes.strings.at_the_bottom_of_the_screen_there_is_a_button_to_switch_the_grip_of_the_customizable_gesture),
                    paragraph(SharedRes.strings.if_the_open_and_closed_positions_of_the_prosthetic_fingers_do_not_match_after_pressing_it_you_will_see_an_animation_of_the_grip_that_will_be_applied_when_using_this_gesture),
                    paragraph(SharedRes.strings.in_real_life_the_prosthetic_will_perform_the_same_finger_movements_as_the_model_on_the_smartphone_screen),
                    paragraph(SharedRes.strings.you_can_also_enter_your_own_name_for_the_gesture_by_clicking_on_the_pencil_icon_next_to_the_default_gesture_name),
                    image(SharedRes.images.ubi4_help_image_gesture_settings_7, 180),
                    heading(SharedRes.strings.saving_settings_and_using_gestures),
                    paragraph(SharedRes.strings.after_reaching_the_desired_open_and_closed_position_of_the_gesture_you_must_press_the_save_button_located_at_the_bottom_of_the_screen),
                    image(SharedRes.images.ubi4_help_image_gesture_settings_8, 180),
                    paragraph(SharedRes.strings.to_use_the_configured_gesture_press_the_button_corresponding_to_the_gesture_it_will_become_white_different_from_all_other_buttons),
                    image(SharedRes.images.ubi4_help_image_gesture_settings_9, 180)
                )
            )
        ),
        relatedItems = relatedAppItems(PAGE_GESTURES)
    )

    private fun advancedPage(): InstructionPage = InstructionPage(
        id = PAGE_ADVANCED,
        title = SharedRes.strings.advanced_settings,
        cards = listOf(
            InstructionCard(
                blocks = listOf(
                    image(SharedRes.images.ubi4_help_image_advanced_settings_1, 180),
                    image(SharedRes.images.ubi4_help_image_advanced_settings_2, 180),
                    image(SharedRes.images.ubi4_help_image_advanced_settings_3, 180),
                    image(SharedRes.images.ubi4_help_image_advanced_settings_4, 180),
                    image(SharedRes.images.ubi4_help_image_advanced_settings_5, 180),
                    image(SharedRes.images.ubi4_help_image_advanced_settings_6, 180),
                    image(SharedRes.images.ubi4_help_image_advanced_settings_7, 180),
                    image(SharedRes.images.ubi4_help_image_advanced_settings_8, 180)
                )
            )
        ),
        relatedItems = relatedAppItems(PAGE_ADVANCED)
    )

    private fun howWorksPage(): InstructionPage = InstructionPage(
        id = PAGE_HOW_WORKS,
        title = SharedRes.strings.how_prostheses_works,
        cards = listOf(
            InstructionCard(
                blocks = listOf(
                    heading(SharedRes.strings.control),
                    paragraph(SharedRes.strings.the_prostheses_is_controlled_by_alternating_tension_of_the_antagonist_muscles_flexor_extensor_the_signal_from_which_is_recorded_by_myographic_sensors_emg_sensors_fixed_in_the_inner_stump_sleeve),
                    paragraph(SharedRes.strings.two_myosensors_are_used_to_control_the_prostheses_dual_channel_control),
                    paragraph(SharedRes.strings.the_prostheses_has_a_passive_rotation_of_the_hand_relative_to_the_forearm),
                    heading(SharedRes.strings.how_it_works),
                    paragraph(SharedRes.strings.the_hand_has_6_independent_degrees_of_freedom_which_means_that_each_finger_is_controlled_by_a_separate_motor_and_the_thumb_by_two_motors_this_makes_it_possible_to_perform_freely_adjustable_gestures_and_to_use_the_grip_for_different_objects_and_actions_with_them),
                    paragraph(SharedRes.strings.the_prostheses_can_memorize_8_different_gestures_the_first_gesture_in_the_prostheses_is_set_by_default_the_fist_the_other_gestures_can_be_set_individually_according_to_your_wishes_you_can_switch_and_customize_the_gestures_through_the_mobile_app_under_the_gesture_settings_tab),
                    paragraph(SharedRes.strings.you_can_find_out_more_about_working_with_gestures_by_going_to_the_setting_gestures_section_of_the_instructions)
                )
            )
        ),
        relatedItems = relatedProsthesesItems(PAGE_HOW_WORKS)
    )

    private fun socketPage(): InstructionPage = InstructionPage(
        id = PAGE_SOCKET,
        title = SharedRes.strings.how_to_put_on_a_prostheses_socket,
        cards = listOf(
            InstructionCard(
                blocks = listOf(
                    heading(SharedRes.strings.how_to_put_on_a_prostheses_socket_),
                    paragraph(SharedRes.strings.the_prostheses_is_fixed_and_held_in_place_by_the_anatomical_construction_of_the_socket_and_shoulder_brace),
                    notice(SharedRes.strings.important_the_prostheses_must_be_turned_off_when_you_put_it_on),
                    numbered(
                        SharedRes.strings.check_that_the_residual_limb_is_comfortably_positioned_in_the_socket_and_that_it_is_securely_fastened_once_it_is_securely_in_place_turn_on_the_prostheses,
                        SharedRes.strings.press_the_button_for_2_seconds_to_turn_the_denture_on_or_off_as_shown_in_the_illustration
                    ),
                    image(SharedRes.images.help_image, 220)
                )
            )
        ),
        relatedItems = relatedProsthesesItems(PAGE_SOCKET)
    )

    private fun completeSetPage(): InstructionPage = InstructionPage(
        id = PAGE_COMPLETE_SET,
        title = SharedRes.strings.complete_set,
        cards = listOf(
            InstructionCard(
                blocks = listOf(
                    heading(SharedRes.strings.what_s_included_in_the_package),
                    numbered(
                        SharedRes.strings.hand_module,
                        SharedRes.strings.battery_pack,
                        SharedRes.strings.power_system,
                        SharedRes.strings.prosthetic_socket,
                        SharedRes.strings.battery_charger_with_220v_50hz_power_supply,
                        SharedRes.strings.stationary_part_of_the_rotator,
                        SharedRes.strings.sensor_tabs,
                        SharedRes.strings.electromyographic_sensors_with_control_system,
                        SharedRes.strings.specifications,
                        SharedRes.strings.user_manual,
                        SharedRes.strings.button_with_charging_connector,
                        SharedRes.strings.pouch_for_pulling_the_prostheses_through_clothing_sleeves,
                        SharedRes.strings.antiseptic
                    )
                )
            )
        ),
        relatedItems = relatedProsthesesItems(PAGE_COMPLETE_SET)
    )

    private fun chargingPage(): InstructionPage = InstructionPage(
        id = PAGE_CHARGING,
        title = SharedRes.strings.prostheses_charge,
        cards = listOf(
            InstructionCard(
                blocks = listOf(
                    heading(SharedRes.strings.prostheses_charge),
                    notice(SharedRes.strings.important_do_not_charge_the_prostheses_on_yourself),
                    paragraph(SharedRes.strings.the_prostheses_will_not_turn_on_until_charging_is_complete_to_charge_the_prostheses_plug_the_type_c_cable_into_the_jack_on_the_button_the_type_c_cable_must_be_powered_by_a_usb_charger_with_a_voltage_output_of_5v_and_a_current_output_of_2a),
                    paragraph(SharedRes.strings.the_prostheses_should_be_recharged_at_least_once_every_2_months),
                    image(SharedRes.images.help_image_2, 180),
                    heading(SharedRes.strings.charge_level),
                    paragraph(SharedRes.strings.the_battery_status_is_displayed_on_the_power_button),
                    bullets(
                        SharedRes.strings.while_charging,
                        SharedRes.strings.charge_level_100_30,
                        SharedRes.strings.charge_level_30_5,
                        SharedRes.strings.charge_level_is_less_than_5
                    ),
                    image(SharedRes.images.help_image_3, 48),
                    image(SharedRes.images.help_image_4, 48),
                    image(SharedRes.images.help_image_5, 48),
                    image(SharedRes.images.help_image_6, 48)
                )
            )
        ),
        relatedItems = relatedProsthesesItems(PAGE_CHARGING)
    )

    private fun carePage(): InstructionPage = InstructionPage(
        id = PAGE_CARE,
        title = SharedRes.strings.prostheses_care,
        cards = listOf(
            InstructionCard(
                blocks = listOf(
                    heading(SharedRes.strings.prostheses_care),
                    paragraph(SharedRes.strings.check_the_technical_condition_of_your_prostheses_at_regular_intervals_if_you_find_faults_cracks_or_other_malfunctions_contact_the_service_center_or_your_dentist),
                    paragraph(SharedRes.strings.regular_maintenance_of_the_prostheses_is_required_refer_to_service_and_warranty_for_service_intervals),
                    paragraph(SharedRes.strings.it_is_recommended_to_clean_the_arm_prostheses_regularly_use_a_squeezed_cotton_swab_soaked_in_3_hydrogen_peroxide_solution_or_moist_alcohol_wipes_for_this_purpose),
                    notice(SharedRes.strings.do_not_use_aggressive_detergents),
                    paragraph(SharedRes.strings.if_you_have_a_cosmetic_shell_installed_check_it_regularly_for_through_damage_try_to_avoid_contact_with_sharp_or_pointed_objects_if_the_cosmetic_shell_needs_to_be_replaced_please_contact_your_prosthetic_company),
                    notice(SharedRes.strings.important_do_not_try_to_repair_your_prosthetic_arm_yourself_it_may_cause_more_serious_damage_and_void_your_warranty)
                )
            )
        ),
        relatedItems = relatedProsthesesItems(PAGE_CARE)
    )

    private fun servicePage(): InstructionPage = InstructionPage(
        id = PAGE_SERVICE,
        title = SharedRes.strings.service_warranty,
        cards = listOf(
            InstructionCard(
                blocks = listOf(
                    heading(SharedRes.strings.service),
                    paragraph(SharedRes.strings.the_product_is_sold_in_accordance_with_federal_law_2300_1_of_february_7_1992_on_protection_of_consumer_rights),
                    paragraph(SharedRes.strings.when_you_contact_the_service_department_an_examination_is_performed_to_determine_if_there_is_a_warranty_claim_any_repair_or_maintenance_is_performed_by_a_certified_service_center_self_repair_is_not_allowed),
                    paragraph(SharedRes.strings.preventive_maintenance_should_be_performed_every_6_months_or_every_50_000_cycles_whichever_comes_first),
                    heading(SharedRes.strings.service_warranty),
                    paragraph(SharedRes.strings.the_warranty_covers_any_defects_in_materials_or_workmanship_under_normal_use_during_the_warranty_period),
                    paragraph(SharedRes.strings.the_warranty_period_is_2_years_and_is_calculated_from_the_beginning_of_exploitation),
                    paragraph(SharedRes.strings.the_start_date_of_use_is_the_date_the_prostheses_is_handed_over_to_the_user),
                    paragraph(SharedRes.strings.the_prosthetic_arm_is_accepted_under_warranty_only_together_with_this_manual),
                    heading(SharedRes.strings.the_warranty_does_not_apply_to),
                    bullets(
                        SharedRes.strings.battery_pack,
                        SharedRes.strings.stem_sleeve_if_not_included_in_the_package,
                        SharedRes.strings.charger
                    ),
                    notice(SharedRes.strings.warning_the_warranty_does_not_apply_in_the_following_cases),
                    bullets(
                        SharedRes.strings.violations_of_storage_and_operating_conditions_specified_in_this_passport_and_operating_manual,
                        SharedRes.strings.the_presence_of_traces_of_unauthorized_by_the_manufacturer_disassembly_of_the_product_violation_of_the_integrity_of_the_product_and_or_direct_changes_in_the_equipment_of_the_product_without_the_consent_of_the_manufacturer,
                        SharedRes.strings.violations_of_the_integrity_of_protective_seals_and_or_indicator_varnish_coatings,
                        SharedRes.strings.failure_to_complete_routine_maintenance_in_a_timely_manner,
                        SharedRes.strings.the_presence_of_traces_of_exposure_to_corrosive_substances,
                        SharedRes.strings.presence_of_traces_of_damage_to_the_product_by_the_user_and_or_third_parties_chips_dents_falling_from_a_height_of_more_than_1_m_on_a_hard_surface_etc,
                        SharedRes.strings.any_damage_caused_by_foreign_particles_and_water_entering_the_hand_module_the_sensor_or_the_battery_pack,
                        SharedRes.strings.the_presence_of_damage_caused_by_fire_natural_disasters_and_other_force_majeure_circumstances,
                        SharedRes.strings.self_detaching_the_hand_module_from_the_elbow_module
                    ),
                    heading(SharedRes.strings.not_allowed),
                    bullets(
                        SharedRes.strings.connect_the_product_to_damaged_electrical_power_or_source_not_specified_by_the_manufacturer,
                        SharedRes.strings.charge_the_user_s_prostheses,
                        SharedRes.strings.if_the_prostheses_has_not_been_charged_for_more_than_2_months_it_is_necessary_to_contact_the_service,
                        SharedRes.strings.use_a_defective_denture,
                        SharedRes.strings.use_the_product_to_operate_vehicles_dangerous_machinery_and_weapons,
                        SharedRes.strings.use_the_product_to_carry_dangerous_loads_that_can_cause_damage_if_dropped_pressured_or_ignited,
                        SharedRes.strings.do_not_use_the_product_for_purposes_other_than_those_for_which_it_is_intended_or_in_cases_that_could_cause_any_damage_to_the_prostheses,
                        SharedRes.strings.immersion_in_water_and_washing_the_prostheses_under_water_jets_is_prohibited_the_prostheses_is_ip_54_which_means_it_can_be_used_in_the_rain,
                        SharedRes.strings.disassemble_and_repair_the_product_yourself
                    )
                )
            )
        ),
        relatedItems = relatedProsthesesItems(PAGE_SERVICE)
    )

    private fun heading(text: StringResource): InstructionBlock =
        InstructionBlock(InstructionBlockType.HEADING, text, null, emptyList(), 0)

    private fun paragraph(text: StringResource): InstructionBlock =
        InstructionBlock(InstructionBlockType.PARAGRAPH, text, null, emptyList(), 0)

    private fun notice(text: StringResource): InstructionBlock =
        InstructionBlock(InstructionBlockType.NOTICE, text, null, emptyList(), 0)

    private fun numbered(vararg items: StringResource): InstructionBlock =
        InstructionBlock(InstructionBlockType.NUMBERED, null, null, items.toList(), 0)

    private fun bullets(vararg items: StringResource): InstructionBlock =
        InstructionBlock(InstructionBlockType.BULLETS, null, null, items.toList(), 0)

    private fun image(image: ImageResource, height: Int): InstructionBlock =
        InstructionBlock(InstructionBlockType.IMAGE, null, image, emptyList(), height)
}
