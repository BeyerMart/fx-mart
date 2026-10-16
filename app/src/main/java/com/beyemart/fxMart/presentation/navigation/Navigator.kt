package com.beyemart.fxMart.presentation.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(val state: NavigationState) {
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            if (state.topLevelRoute == route) {
                val stack = state.backStacks[route]
                while (stack?.last() != route) {
                    stack?.removeLastOrNull()
                }
            }
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute] ?: return
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute) {
            if (state.topLevelRoute != state.startRoute) {
                state.topLevelRoute = state.startRoute
            }
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
